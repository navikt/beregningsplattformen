import {
	CheckmarkCircleFillIcon,
	ChevronDownIcon,
	ChevronLeftIcon,
	ChevronRightIcon,
	PadlockLockedFillIcon,
	PauseFillIcon,
	PersonIcon,
	PlusIcon,
	ClockDashedIcon,
	XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import {
	BodyShort,
	Box,
	Button,
	CopyButton,
	Heading,
	HGrid,
	HStack,
	Label,
	LocalAlert,
	Timeline,
	ToggleGroup,
	Tooltip,
	VStack,
} from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useRef, useState } from "react";
import { postBeregn } from "../api/apiService";
import { Beregningsresultater } from "../components/Beregningsresultater";
import {
	FellesBeregningsfelt,
	type FellesFeltfeil,
	type FellesOpplysninger,
} from "../components/FellesBeregningsfelt";
import { OppdragsXmlSkjema } from "../components/OppdragsXmlSkjema";
import { SkattekortSkjema } from "../components/SkattekortSkjema";
import { oppdragsXmlTypedTemplate } from "../data/oppdragsXmlTypedTemplate";
import { HttpStatusCodeError } from "../types/Error";
import type { BeregningResponse } from "../types/BeregningResponse";
import type { OppdragstidslinjeDTO } from "../types/OppdragstidslinjeDTO";
import { type SkattekortUtkast, lesSkattekort, nyttForskuddstrekk } from "../types/Skattekort";
import {
	fillTypedTemplate,
	templateFields,
	xmlMedGjelderId,
} from "../util/template";
import styles from "./SendInnOppdragsXML.module.css";

interface XmlUtkast {
	id: number;
	xmlMal: string;
	feltverdier: Record<string, string>;
}

type KjedePeriodeDTO =
	OppdragstidslinjeDTO["kjeder"][number]["kjedeVersjoner"][number]["kjedePerioder"][number];

function formaterFeltnavn(feltnavn: string) {
	const medMellomrom = feltnavn
		.replace(/([A-Z])/g, " $1")
		.toLowerCase()
		.trim();
	return medMellomrom.charAt(0).toUpperCase() + medMellomrom.slice(1);
}

function formaterNorskDato(dato: string) {
	const [år, måned, dag] = dato.split("-");
	return `${dag}.${måned}.${år}`;
}

function formaterPeriodeTekst(periode: KjedePeriodeDTO) {
	const fra = formaterNorskDato(periode.fraDato);
	if (periode.tilDato === null) return `${fra} - ∞`;
	return `${fra} - ${formaterNorskDato(periode.tilDato)}`;
}

function erOpplysning(
	verdi: unknown,
): verdi is { saksbehandlerId: string; opplysning: string } {
	if (typeof verdi !== "object" || verdi === null) return false;
	const kandidat = verdi as Record<string, unknown>;
	return (
		typeof kandidat.opplysning === "string" &&
		typeof kandidat.saksbehandlerId === "string"
	);
}

function formaterKortFagsystemId(fagsystemId: string) {
	return fagsystemId.length > 16
		? `${fagsystemId.slice(0, 8)}…${fagsystemId.slice(-4)}`
		: fagsystemId;
}

function visualiseringForStatus(status: string): {
	status: "success" | "warning" | "danger" | "info" | "neutral";
	icon: React.ReactNode;
	statusLabel: string;
} {
	switch (status.trim().toLowerCase()) {
		case "aktiv":
			return {
				status: "success",
				icon: <CheckmarkCircleFillIcon aria-hidden />,
				statusLabel: "Aktiv",
			};
		case "opphørt":
			return {
				status: "danger",
				icon: <XMarkOctagonFillIcon aria-hidden />,
				statusLabel: "Opphørt",
			};
		case "hvilende":
			return {
				status: "neutral",
				icon: <PauseFillIcon aria-hidden />,
				statusLabel: "Hvilende",
			};
		case "sperret":
			return {
				status: "neutral",
				icon: <PadlockLockedFillIcon aria-hidden />,
				statusLabel: "Sperret",
			};
		default:
			return {
				status: "info",
				icon: <PauseFillIcon aria-hidden />,
				statusLabel: status,
			};
	}
}

function statusFraPeriode(periode: KjedePeriodeDTO) {
	if (erOpplysning(periode.status)) return periode.status.opplysning;
	return "";
}

function Detaljkort({
	label,
	verdi,
	saksbehandlerId,
}: {
	label: string;
	verdi: string;
	saksbehandlerId?: string;
}) {
	return (
		<VStack
			as="div"
			gap="space-4"
			className={styles.laboratorium__detaljkort}
		>
			<dt>
				<Label as="span" size="small" textColor="subtle">
					{label}
				</Label>
			</dt>
			{saksbehandlerId && (
				<Tooltip content={`Saksbehandler: ${saksbehandlerId}`}>
					<HStack
						as="span"
						align="center"
						position="absolute"
						top="space-12"
						right="space-12"
						className={styles.laboratorium__saksbehandlerikon}
						// biome-ignore lint/a11y/noNoninteractiveTabindex: Gjør saksbehandler-tooltipet tilgjengelig for tastatur.
						tabIndex={0}
						role="note"
						aria-label={`Saksbehandler: ${saksbehandlerId}`}
					>
						<PersonIcon aria-hidden />
					</HStack>
				</Tooltip>
			)}
			<dd>
				<HStack
					as="span"
					align="center"
					gap="space-8"
					className={styles.laboratorium__detaljverdi}
				>
					<span className={styles.laboratorium__detaljverditekst}>{verdi}</span>
					<CopyButton
						size="xsmall"
						copyText={verdi}
						aria-label={`Kopier ${label}`}
					/>
				</HStack>
			</dd>
		</VStack>
	);
}

export default function SendInnOppdragsXML() {
	const [fellesOpplysninger, settFellesOpplysninger] = useState<FellesOpplysninger>(
		() => ({
			gjelderId:
				templateFields(oppdragsXmlTypedTemplate).find(
					(felt) => felt.name === "oppdragGjelderId",
				)?.value ?? "",
			fom: "",
			tom: "",
			fomGyldig: true,
			tomGyldig: true,
		}),
	);
	const [fellesfeil, settFellesfeil] = useState<FellesFeltfeil[]>([]);
	const [skattekortUtkast, settSkattekortUtkast] = useState<SkattekortUtkast>({
		aktiv: false,
		inntektsaar: String(new Date().getFullYear()),
		forskuddstrekk: nyttForskuddstrekk(),
	});
	const [xmlUtkast, settXmlUtkast] = useState<XmlUtkast[]>([
		{ id: 1, xmlMal: oppdragsXmlTypedTemplate, feltverdier: {} },
	]);
	const [valgtXmlId, settValgtXmlId] = useState(1);
	const nesteXmlId = useRef(2);
	const senderRef = useRef(false);
	const [sender, settSender] = useState(false);
	const [resultat, settResultat] = useState<string | null>(null);
	const [feilmelding, settFeilmelding] = useState<string | null>(null);
	const [respons, settRespons] = useState<BeregningResponse | null>(null);
	const [xmlFeilId, settXmlFeilId] = useState<number | null>(null);
	const [utvidedeKjeder, settUtvidedeKjeder] = useState<Set<string>>(
		() => new Set(),
	);
	const [valgtPeriode, settValgtPeriode] = useState<{
		id: string;
		kjedeId: string;
		versjonsindeks: number;
		periodeindeks: number;
		fagområde: string;
		fagsystemId: string;
		periode: KjedePeriodeDTO;
		historisk: boolean;
	} | null>(null);
	const [tidslinjeMåneder, settTidslinjeMåneder] = useState(4);
	const kjeder = respons?.oppdragstidslinje.kjeder ?? [];
	const allePerioder = kjeder.flatMap((kjede) =>
		kjede.kjedeVersjoner.flatMap((versjon) => versjon.kjedePerioder),
	);
	const [tidslinjeStart, settTidslinjeStart] = useState<Date | null>(null);
	const tidslinjeSlutt = tidslinjeStart
		? new Date(
				tidslinjeStart.getFullYear(),
				tidslinjeStart.getMonth() + tidslinjeMåneder,
				0,
			)
		: null;
	const flyttTidslinje = (antallMåneder: number) => {
		settTidslinjeStart((start) =>
			start
				? new Date(start.getFullYear(), start.getMonth() + antallMåneder, 1)
				: start,
		);
	};
	const fjernResultat = () => {
		settResultat(null);
		settFeilmelding(null);
		settRespons(null);
		settValgtPeriode(null);
		settUtvidedeKjeder(new Set());
		settTidslinjeStart(null);
		settXmlFeilId(null);
	};
	const endreMal = (id: number, xmlMal: string) => {
		fjernResultat();
		const felter = templateFields(xmlMal);
		settXmlUtkast((utkast) =>
			utkast.map((draft) =>
				draft.id === id
					? {
							...draft,
							xmlMal,
							feltverdier: Object.fromEntries(
								felter.map((felt) => [
									felt.key,
									draft.feltverdier[felt.key] ?? felt.value,
								]),
							),
						}
					: draft,
			),
		);
	};
	const endreFeltverdi = (id: number, key: string, value: string) => {
		fjernResultat();
		settXmlUtkast((utkast) =>
			utkast.map((draft) =>
				draft.id === id
					? { ...draft, feltverdier: { ...draft.feltverdier, [key]: value } }
					: draft,
			),
		);
	};
	const nullstillFelter = (id: number) => {
		fjernResultat();
		settXmlUtkast((utkast) =>
			utkast.map((draft) =>
				draft.id === id ? { ...draft, feltverdier: {} } : draft,
			),
		);
	};
	const tilbakestillMal = (id: number) => {
		fjernResultat();
		settXmlUtkast((utkast) =>
			utkast.map((draft) =>
				draft.id === id
					? {
							...draft,
							xmlMal: oppdragsXmlTypedTemplate,
							feltverdier: {},
						}
					: draft,
			),
		);
	};
	const handterInnsending = async (hendelse: SubmitEvent<HTMLFormElement>) => {
		hendelse.preventDefault();
		if (senderRef.current) return;

		fjernResultat();
		const { gjelderId, fom, tom, fomGyldig, tomGyldig } = fellesOpplysninger;
		const feltfeil: FellesFeltfeil[] = [];
		if (!/^\d{11}$/.test(gjelderId.trim())) {
			feltfeil.push({
				felt: "gjelderId",
				melding: "Oppgi en syntetisk gjelder-ID med 11 sifre.",
			});
		}
		if (!fomGyldig) {
			feltfeil.push({ felt: "fom", melding: "Oppgi en gyldig fra-dato." });
		}
		if (!tomGyldig) {
			feltfeil.push({ felt: "tom", melding: "Oppgi en gyldig til-dato." });
		} else if (fom && tom && fom > tom) {
			feltfeil.push({
				felt: "tom",
				melding: "Til-datoen må være lik eller etter fra-datoen.",
			});
		}
		settFellesfeil(feltfeil);
		if (feltfeil.length > 0) return;

		const xmlPerUtkast: string[] = [];
		for (const draft of xmlUtkast) {
			const xml = fillTypedTemplate(
				draft.xmlMal,
				templateFields(draft.xmlMal),
				draft.feltverdier,
			);
			const oppdragXml = xmlMedGjelderId(xml, gjelderId.trim());
			if (!oppdragXml.gyldig) {
				settValgtXmlId(draft.id);
				settXmlFeilId(draft.id);
				settResultat(
					`XML-utkast ${draft.id}: ${xml.trim() ? oppdragXml.feil : "Mangler XML-innhold."}`,
				);
				return;
			}
			xmlPerUtkast.push(oppdragXml.xml);
		}

		senderRef.current = true;
		settSender(true);
		try {
			const response = await postBeregn({
				xml: xmlPerUtkast,
				...(fom ? { fom } : {}),
				...(tom ? { tom } : {}),
				...(skattekortUtkast.aktiv
					? { skattekort: lesSkattekort(skattekortUtkast) }
					: {}),
			});
			settRespons(response);
			const perioder = response.oppdragstidslinje.kjeder.flatMap((kjede) =>
				kjede.kjedeVersjoner.flatMap((versjon) => versjon.kjedePerioder),
			);
			if (perioder.length === 0) {
				settTidslinjeStart(null);
			} else {
				const tidligsteDato = new Date(
					Math.min(
						...perioder.map((periode) => new Date(periode.fraDato).getTime()),
					),
				);
				settTidslinjeStart(
					new Date(tidligsteDato.getFullYear(), tidligsteDato.getMonth(), 1),
				);
			}
			settResultat("XML-ene er behandlet.");
		} catch (error) {
			if (error instanceof HttpStatusCodeError && error.apiMessage) {
				settFeilmelding(error.apiMessage);
			}
			settResultat(
				error instanceof HttpStatusCodeError ||
					(isAxiosError(error) && error.response)
					? "Kunne ikke behandle XML-ene. Kontroller innholdet og prøv igjen."
					: "Kunne ikke behandle XML-ene. Prøv igjen.",
			);
		} finally {
			senderRef.current = false;
			settSender(false);
		}
	};

	return (
		<VStack
			as="main"
			align="center"
			gap="space-32"
			padding="space-32"
			className={styles.laboratorium}
		>
			<Box
				as="section"
				className={styles["laboratorium__panel--xml"]}
				width="100%"
				maxWidth="90rem"
				padding="space-32"
				borderWidth="1"
				borderColor="neutral-subtle"
				borderRadius="12"
				aria-labelledby="lab-heading"
			>
				<BodyShort className={styles.laboratorium__intro}>
					Fyll ut feltene for å bygge et XML-oppdrag, forhåndsvis resultatet og
					behandle det.
				</BodyShort>
				<LocalAlert className={styles.laboratorium__advarsel} status="warning">
					<LocalAlert.Header>
						<LocalAlert.Title>Bruk bare syntetiske XML-data</LocalAlert.Title>
					</LocalAlert.Header>
					<LocalAlert.Content>
						Ikke lim inn personopplysninger eller reelle ID-er.
					</LocalAlert.Content>
				</LocalAlert>
				<form onSubmit={handterInnsending} aria-busy={sender}>
					<FellesBeregningsfelt
						verdi={fellesOpplysninger}
						feil={fellesfeil}
						sender={sender}
						onChange={(endringer) => {
							fjernResultat();
							settFellesfeil([]);
							settFellesOpplysninger((verdi) => ({ ...verdi, ...endringer }));
						}}
					/>
					<HStack
						as="fieldset"
						align="center"
						gap="space-8"
						wrap
						marginBlock="space-0 space-16"
						className={styles.laboratorium__xmlvalg}
					>
						<legend className="aksel-sr-only">Velg XML-utkast</legend>
						{xmlUtkast.map(({ id }) => (
							<Button
								key={id}
								type="button"
								variant={valgtXmlId === id ? "primary" : "secondary"}
								aria-pressed={valgtXmlId === id}
								onClick={() => settValgtXmlId(id)}
							>
								XML {id}
							</Button>
						))}
					</HStack>
					{xmlUtkast.map((draft) => (
						<div key={draft.id} hidden={valgtXmlId !== draft.id}>
							<OppdragsXmlSkjema
								nummer={draft.id}
								xmlMal={draft.xmlMal}
								feltverdier={draft.feltverdier}
								xmlFeil={xmlFeilId === draft.id}
								sender={sender}
								onXmlMalChange={(xmlMal) => endreMal(draft.id, xmlMal)}
								onFeltverdiChange={(key, value) =>
									endreFeltverdi(draft.id, key, value)
								}
								onNullstillFelter={() => nullstillFelter(draft.id)}
								onTilbakestillMal={() => tilbakestillMal(draft.id)}
							/>
						</div>
					))}
					<HStack justify="end" marginBlock="space-16 space-0">
						<Button
							type="button"
							variant="secondary"
							icon={<PlusIcon aria-hidden />}
							disabled={sender}
							onClick={() => {
								fjernResultat();
								const nyttXmlId = nesteXmlId.current;
								nesteXmlId.current += 1;
								settXmlUtkast((utkast) => [
									...utkast,
									{
										id: nyttXmlId,
										xmlMal: oppdragsXmlTypedTemplate,
										feltverdier: {},
									},
								]);
								settValgtXmlId(nyttXmlId);
							}}
						>
							Legg til XML
						</Button>
					</HStack>
					<SkattekortSkjema
						utkast={skattekortUtkast}
						sender={sender}
						onChange={(utkast) => {
							fjernResultat();
							settSkattekortUtkast(utkast);
						}}
					/>
					<HStack justify="end" marginBlock="space-16 space-0">
						<Button type="submit" loading={sender} disabled={sender}>
							Behandle alle XML
						</Button>
					</HStack>
					{resultat && (
						<BodyShort
							role="status"
							aria-live="polite"
						>
							{resultat}
						</BodyShort>
					)}
				</form>
			</Box>
			<VStack
				as="section"
				gap="space-16"
				className={styles.laboratorium__tidslinje}
				width="100%"
				maxWidth="90rem"
				padding="space-32"
				aria-labelledby="tidslinje-heading"
			>
				<Heading id="tidslinje-heading" level="2" size="medium">
					Tidslinjer
				</Heading>
				{feilmelding ? (
					<LocalAlert status="error">
						<LocalAlert.Header>
							<LocalAlert.Title>
								Kunne ikke behandle innsendingen
							</LocalAlert.Title>
						</LocalAlert.Header>
						<LocalAlert.Content className={styles.laboratorium__feilmelding}>
							{feilmelding}
						</LocalAlert.Content>
					</LocalAlert>
				) : allePerioder.length === 0 ? (
					<BodyShort>Ingen tidslinjer å vise.</BodyShort>
				) : (
					tidslinjeStart &&
					tidslinjeSlutt && (
						<>
							<HStack justify="end" align="center" gap="space-8">
								<ToggleGroup
									aria-label="Antall måneder i perioden"
									data-color="neutral"
									size="small"
									value={String(tidslinjeMåneder)}
									onChange={(value) => settTidslinjeMåneder(Number(value))}
								>
									<ToggleGroup.Item value="2" label="2 måneder" />
									<ToggleGroup.Item value="4" label="4 måneder" />
									<ToggleGroup.Item value="8" label="8 måneder" />
								</ToggleGroup>
								<HStack gap="space-4" wrap={false}>
									<Button
										type="button"
										data-color="neutral"
										variant="secondary"
										size="small"
										icon={<ChevronLeftIcon aria-hidden />}
										aria-label="Forrige periode"
										onClick={() => flyttTidslinje(-tidslinjeMåneder)}
									/>
									<Button
										type="button"
										data-color="neutral"
										variant="secondary"
										size="small"
										icon={<ChevronRightIcon aria-hidden />}
										aria-label="Neste periode"
										onClick={() => flyttTidslinje(tidslinjeMåneder)}
									/>
								</HStack>
							</HStack>
							<Box
								as="section"
								minWidth="0"
								padding="space-16"
								borderWidth="2"
								borderColor="neutral"
								borderRadius="12"
								className={styles.laboratorium__kjede}
							>
								<Timeline startDate={tidslinjeStart} endDate={tidslinjeSlutt}>
									{kjeder.flatMap((kjede) => {
										const aktivVersjon = kjede.kjedeVersjoner[0];
										if (!aktivVersjon) return [];

										const harHistorikk = kjede.kjedeVersjoner.length > 1;
										const erUtvidet = utvidedeKjeder.has(kjede.id);
										const kjedeId = encodeURIComponent(kjede.id);

										const lagPeriode = (
											periode: (typeof aktivVersjon.kjedePerioder)[number],
											versjonsindeks: number,
											periodeindeks: number,
											historisk: boolean,
										) => {
											const periodeId = `periode-kjede-${kjedeId}-${versjonsindeks}-${periodeindeks}`;
											const periodeStatus = visualiseringForStatus(
												statusFraPeriode(periode),
											);
											const periodeIkon = historisk ? (
												<ClockDashedIcon aria-hidden />
											) : (
												periodeStatus.icon
											);
											const periodeTekst = formaterPeriodeTekst(periode);
											const statusTekst = statusFraPeriode(periode);

											return (
												<Timeline.Period
													key={periodeId}
													id={periodeId}
													aria-controls="periodedetaljer"
													isActive={valgtPeriode?.id === periodeId}
													onSelectPeriod={() =>
														settValgtPeriode((previous) =>
															previous?.id === periodeId
																? null
																: {
																		id: periodeId,
																		kjedeId: kjede.id,
																		versjonsindeks,
																		periodeindeks,
																		fagområde: kjede.fagområde,
																		fagsystemId: kjede.fagsystemId,
																		periode,
																		historisk,
																	},
														)
													}
													start={new Date(periode.fraDato)}
													end={
														periode.tilDato
															? new Date(periode.tilDato)
															: new Date()
													}
													status={periodeStatus.status}
													icon={periodeIkon}
													statusLabel={periodeStatus.statusLabel}
													title={periodeTekst}
													className={
														historisk
															? styles.laboratorium__historikkperiode
															: undefined
													}
												>
													<p>{periodeTekst}</p>
													{statusTekst && <p>Status: {statusTekst}</p>}
												</Timeline.Period>
											);
										};

										const historikkRader =
											harHistorikk && erUtvidet
												? kjede.kjedeVersjoner
														.slice(1)
														.map((versjon, indeks) => {
															const versjonsindeks = indeks + 1;

															return (
																<Timeline.Row
																	key={`rad-kjede-${kjedeId}-${versjonsindeks}`}
																	id={`rad-kjede-${kjedeId}-${versjonsindeks}`}
																	label={
																		<BodyShort
																			as="span"
																			size="small"
																			textColor="subtle"
																			className={
																				styles.laboratorium__historikketikett
																			}
																		>
																			Etter innsending {versjon.innsending}
																		</BodyShort>
																	}
																>
																	{versjon.kjedePerioder.map(
																		(periode, periodeindeks) =>
																			lagPeriode(
																				periode,
																				versjonsindeks,
																				periodeindeks,
																				true,
																			),
																	)}
																</Timeline.Row>
															);
														})
												: [];

										return [
											<Timeline.Row
												key={`rad-kjede-${kjedeId}-0`}
												id={`rad-kjede-${kjedeId}-0`}
												label={
													<HStack as="div" align="center" gap="space-8">
														<VStack as="div" gap="space-4">
															<div
																className={
																	styles.laboratorium__tidslinjemetadata
																}
															>
																<BodyShort
																	as="h3"
																	size="small"
																	weight="semibold"
																>
																	{kjede.fagområde}
																</BodyShort>
																<HStack as="div" align="center" gap="space-4">
																	<Tooltip
																		content={`Fagsystem-ID: ${kjede.fagsystemId}`}
																		maxChar={Number.MAX_SAFE_INTEGER}
																	>
																		<span
																			role="note"
																			// biome-ignore lint/a11y/noNoninteractiveTabindex: Keeps the full-ID tooltip available to keyboard users.
																			tabIndex={0}
																			aria-label={`Fagsystem-ID: ${kjede.fagsystemId}`}
																		>
																			{formaterKortFagsystemId(
																				kjede.fagsystemId,
																			)}
																		</span>
																	</Tooltip>
																	<CopyButton
																		size="small"
																		copyText={kjede.fagsystemId}
																		aria-label="Kopier full fagsystem-ID"
																	/>
																	{harHistorikk && (
																		<Button
																			type="button"
																			size="xsmall"
																			variant="tertiary"
																			icon={
																				erUtvidet ? (
																					<ChevronDownIcon aria-hidden />
																				) : (
																					<ChevronRightIcon aria-hidden />
																				)
																			}
																			aria-label={`${erUtvidet ? "Skjul" : "Vis"} historikk for ${kjede.fagområde}`}
																			aria-expanded={erUtvidet}
																			onClick={() => {
																				if (
																					erUtvidet &&
																					valgtPeriode?.kjedeId === kjede.id &&
																					valgtPeriode.historisk
																				) {
																					settValgtPeriode(null);
																				}
																				settUtvidedeKjeder((utvidede) => {
																					const nyeUtvidede = new Set(utvidede);
																					if (nyeUtvidede.has(kjede.id)) {
																						nyeUtvidede.delete(kjede.id);
																					} else {
																						nyeUtvidede.add(kjede.id);
																					}
																					return nyeUtvidede;
																				});
																			}}
																		/>
																	)}
																</HStack>
															</div>
														</VStack>
													</HStack>
												}
											>
												{aktivVersjon.kjedePerioder.map(
													(periode, periodeindeks) =>
														lagPeriode(periode, 0, periodeindeks, false),
												)}
											</Timeline.Row>,
											...historikkRader,
										];
									})}
								</Timeline>
							</Box>
						</>
					)
				)}
				<Box
					id="periodedetaljer"
					aria-controls={valgtPeriode?.id}
					aria-live="polite"
				>
					{valgtPeriode && (
						<Box
							as="section"
							padding="space-24"
							borderWidth="2"
							borderRadius="12"
							className={styles.laboratorium__periodedetaljer}
							aria-labelledby="periodedetaljer-heading"
						>
							<Heading id="periodedetaljer-heading" level="3" size="small">
								Detaljer for valgt periode
							</Heading>
							<HGrid
								as="dl"
								columns="repeat(auto-fit, minmax(min(100%, 14rem), 1fr))"
								gap="space-12"
								marginBlock="space-20 space-0"
							>
								<Detaljkort label="Fagområde" verdi={valgtPeriode.fagområde} />
								<Detaljkort
									label="Fagsystem-ID"
									verdi={valgtPeriode.fagsystemId}
								/>
								<Detaljkort
									label="Periode"
									verdi={formaterPeriodeTekst(valgtPeriode.periode)}
								/>
								{Object.entries(valgtPeriode.periode).map(
									([feltnavn, verdi]) => {
										if (
											feltnavn === "fraDato" ||
											feltnavn === "tilDato" ||
											feltnavn === "status" ||
											verdi === null
										) {
											return null;
										}
										const erOpplysningsverdi = erOpplysning(verdi);
										const visningsverdi = erOpplysningsverdi
											? verdi.opplysning
											: typeof verdi === "string"
												? verdi
												: null;
										if (visningsverdi === null) return null;
										return (
											<Detaljkort
												key={feltnavn}
												label={formaterFeltnavn(feltnavn)}
												verdi={visningsverdi}
												saksbehandlerId={
													erOpplysningsverdi ? verdi.saksbehandlerId : undefined
												}
											/>
										);
									},
								)}
							</HGrid>
							<BodyShort
								size="small"
								textColor="subtle"
								align="end"
								className={styles.laboratorium__kjedeid}
							>
								Kjede-ID: {valgtPeriode.kjedeId}
							</BodyShort>
						</Box>
					)}
				</Box>
			</VStack>
			{respons?.beregningstidslinje && (
				<Beregningsresultater
					beregninger={respons.beregningstidslinje.beregninger}
				/>
			)}
		</VStack>
	);
}
