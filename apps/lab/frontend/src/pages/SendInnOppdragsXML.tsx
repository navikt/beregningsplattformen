import {
	ChevronDownIcon,
	ChevronLeftIcon,
	ChevronRightIcon,
	PlusIcon,
} from "@navikt/aksel-icons";
import {
	BodyShort,
	Box,
	Button,
	Heading,
	LocalAlert,
	Timeline,
	ToggleGroup,
} from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useRef, useState } from "react";
import { postTolkOppdrag } from "../api/apiService";
import { OppdragsXmlSkjema } from "../components/OppdragsXmlSkjema";
import { oppdragsXmlTypedTemplate } from "../data/oppdragsXmlTypedTemplate";
import { HttpStatusCodeError } from "../types/Error";
import type { OppdragstidslinjeDTO } from "../types/OppdragstidslinjeDTO";
import { fillTypedTemplate, templateFields } from "../util/template";
import styles from "./SendInnOppdragsXML.module.css";

interface XmlUtkast {
	id: number;
	xmlMal: string;
	feltverdier: Record<string, string>;
}

type KjedePeriodeDTO =
	OppdragstidslinjeDTO["kjeder"][number]["kjedeVersjoner"][number]["kjedePerioder"][number];

function formaterFeltnavn(feltnavn: string) {
	return feltnavn.replace(/([A-Z])/g, " $1");
}

function formaterFeltverdi(feltnavn: string, verdi: string | null) {
	if (verdi !== null) return verdi;
	if (feltnavn === "tilDato") return "Ingen sluttdato";
	return "Ingen verdi";
}

export default function SendInnOppdragsXML() {
	const [xmlUtkast, settXmlUtkast] = useState<XmlUtkast[]>([
		{ id: 1, xmlMal: oppdragsXmlTypedTemplate, feltverdier: {} },
	]);
	const [valgtXmlId, settValgtXmlId] = useState(1);
	const nesteXmlId = useRef(2);
	const senderRef = useRef(false);
	const [sender, settSender] = useState(false);
	const [resultat, settResultat] = useState<string | null>(null);
	const [respons, settRespons] = useState<OppdragstidslinjeDTO | null>(null);
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
	const kjeder = respons?.kjeder ?? [];
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
		const xmlPerUtkast = xmlUtkast.map((draft) =>
			fillTypedTemplate(
				draft.xmlMal,
				templateFields(draft.xmlMal),
				draft.feltverdier,
			),
		);
		const tomtUtkastIndeks = xmlPerUtkast.findIndex((xml) => !xml.trim());
		if (tomtUtkastIndeks !== -1) {
			const tomtUtkastId = xmlUtkast[tomtUtkastIndeks].id;
			settValgtXmlId(tomtUtkastId);
			settXmlFeilId(tomtUtkastId);
			settResultat(`XML-utkast ${tomtUtkastId} mangler XML-innhold.`);
			return;
		}

		senderRef.current = true;
		settSender(true);
		try {
			const response = await postTolkOppdrag({ xml: xmlPerUtkast });
			settRespons(response);
			const perioder = response.kjeder.flatMap((kjede) =>
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
		<main className={styles.laboratorium}>
			<section
				className={`${styles.laboratorium__panel} ${styles["laboratorium__panel--xml"]}`}
				aria-labelledby="lab-heading"
			>
				<p className={styles.laboratorium__intro}>
					Fyll ut feltene for å bygge et XML-oppdrag, forhåndsvis resultatet og
					behandle det.
				</p>
				<LocalAlert className={styles.laboratorium__advarsel} status="warning">
					<LocalAlert.Header>
						<LocalAlert.Title>Bruk bare syntetiske XML-data</LocalAlert.Title>
					</LocalAlert.Header>
					<LocalAlert.Content>
						Ikke lim inn personopplysninger eller reelle ID-er.
					</LocalAlert.Content>
				</LocalAlert>
				<form onSubmit={handterInnsending} aria-busy={sender}>
					<fieldset className={styles.laboratorium__xmlvalg}>
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
					</fieldset>
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
					<div className={styles.laboratorium__leggtil}>
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
					</div>
					<div className={styles.laboratorium__leggtil}>
						<Button type="submit" loading={sender} disabled={sender}>
							Behandle alle XML
						</Button>
					</div>
					{resultat && (
						<p
							className={styles.laboratorium__resultat}
							role="status"
							aria-live="polite"
						>
							{resultat}
						</p>
					)}
				</form>
			</section>
			<section
				className={`${styles.laboratorium__panel} ${styles.laboratorium__tidslinje}`}
				aria-labelledby="tidslinje-heading"
			>
				<Heading id="tidslinje-heading" level="2" size="medium">
					Tidslinjer
				</Heading>
				{allePerioder.length === 0 ? (
					<BodyShort>Ingen tidslinjer å vise.</BodyShort>
				) : (
					tidslinjeStart &&
					tidslinjeSlutt && (
						<>
							<div className={styles.laboratorium__tidslinjenavigasjon}>
								<BodyShort aria-live="polite">
									Viser {tidslinjeStart.toLocaleDateString("nb-NO")}–
									{tidslinjeSlutt.toLocaleDateString("nb-NO")}
								</BodyShort>
								<div className={styles.laboratorium__tidslinjekontroller}>
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
									<div className={styles.laboratorium__tidslinjepaginering}>
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
									</div>
								</div>
							</div>
							<Timeline startDate={tidslinjeStart} endDate={tidslinjeSlutt}>
								{kjeder.flatMap((kjede, kjedeindeks) => {
									const aktivVersjon = kjede.kjedeVersjoner[0];
									if (!aktivVersjon) return [];

									const harHistorikk = kjede.kjedeVersjoner.length > 1;
									const erUtvidet = utvidedeKjeder.has(kjede.id);
									const kjedeId = encodeURIComponent(kjede.id);
									const kjedeLabel = `Kjede ${kjedeindeks + 1}`;

									const lagPeriode = (
										periode: (typeof aktivVersjon.kjedePerioder)[number],
										versjonsindeks: number,
										periodeindeks: number,
										historisk: boolean,
									) => {
										const periodeId = `periode-kjede-${kjedeId}-${versjonsindeks}-${periodeindeks}`;

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
												status={historisk ? "warning" : "success"}
											>
												{Object.entries(periode).map(([feltnavn, verdi]) => (
													<p key={feltnavn}>
														{formaterFeltnavn(feltnavn)}:{" "}
														{formaterFeltverdi(feltnavn, verdi)}
													</p>
												))}
											</Timeline.Period>
										);
									};

									const historikkRader =
										harHistorikk && erUtvidet
											? kjede.kjedeVersjoner.slice(1).map((versjon, indeks) => {
													const versjonsindeks = indeks + 1;

													return (
														<Timeline.Row
															key={`rad-kjede-${kjedeId}-${versjonsindeks}`}
															id={`rad-kjede-${kjedeId}-${versjonsindeks}`}
															label={
																<div
																	className={
																		styles.laboratorium__tidslinjeetikett
																	}
																>
																	<BodyShort as="h3" size="small">
																		Historikk
																	</BodyShort>
																</div>
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
												<div className={styles.laboratorium__tidslinjeetikett}>
													<BodyShort as="h3" size="small" weight="semibold">
														{kjedeLabel}
													</BodyShort>
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
															aria-label={`${erUtvidet ? "Skjul" : "Vis"} historikk for ${kjedeLabel}`}
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
												</div>
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
						</>
					)
				)}
				<Box
					id="periodedetaljer"
					aria-controls={valgtPeriode?.id}
					aria-live="polite"
				>
					{valgtPeriode && (
						<section
							className={styles.laboratorium__periodedetaljer}
							aria-labelledby="periodedetaljer-heading"
						>
							<Heading id="periodedetaljer-heading" level="3" size="small">
								Detaljer for valgt periode
							</Heading>
							<dl className={styles["laboratorium__periodedetaljer-liste"]}>
								<div>
									<dt>Kjede-ID</dt>
									<dd>{valgtPeriode.kjedeId}</dd>
								</div>
								<div>
									<dt>Fagområde</dt>
									<dd>{valgtPeriode.fagområde}</dd>
								</div>
								<div>
									<dt>Fagsystem-ID</dt>
									<dd>{valgtPeriode.fagsystemId}</dd>
								</div>
								{Object.entries(valgtPeriode.periode).map(([feltnavn, verdi]) => (
									<div key={feltnavn}>
										<dt>{formaterFeltnavn(feltnavn)}</dt>
										<dd>{formaterFeltverdi(feltnavn, verdi)}</dd>
									</div>
								))}
							</dl>
						</section>
					)}
				</Box>
			</section>
		</main>
	);
}
