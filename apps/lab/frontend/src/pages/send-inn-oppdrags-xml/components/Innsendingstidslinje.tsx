import {
	CheckmarkCircleFillIcon,
	ChevronDownIcon,
	ChevronLeftIcon,
	ChevronRightIcon,
	ClockDashedIcon,
	PadlockLockedFillIcon,
	PauseFillIcon,
	PersonIcon,
	XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import {
	BodyShort,
	Box,
	Button,
	CopyButton,
	Detail,
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
import { useState } from "react";
import type { BeregningResponse } from "../../../types/BeregningResponse";
import type { OppdragstidslinjeDTO } from "../../../types/OppdragstidslinjeDTO";
import styles from "./Innsendingstidslinje.module.css";

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

function tidligsteMåned(perioder: KjedePeriodeDTO[]) {
	if (perioder.length === 0) return null;
	const tidligsteDato = new Date(
		Math.min(...perioder.map((periode) => new Date(periode.fraDato).getTime())),
	);
	return new Date(tidligsteDato.getFullYear(), tidligsteDato.getMonth(), 1);
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
		<Box
			background="raised"
			borderRadius="12"
			borderWidth="1"
			borderColor="neutral-subtle"
			padding="space-16"
		>
			<VStack gap="space-4">
				<HStack gap="space-4">
					<Detail uppercase>
						<Label size="small" textColor="subtle">
							{label}
						</Label>
					</Detail>
					{saksbehandlerId && (
						<Tooltip content={`Saksbehandler: ${saksbehandlerId}`}>
							<PersonIcon aria-hidden />
						</Tooltip>
					)}
				</HStack>
				<HStack align="center" gap="space-8">
					<BodyShort textColor="subtle">{verdi}</BodyShort>
					<CopyButton
						size="xsmall"
						copyText={verdi}
						aria-label={`Kopier ${label}`}
					/>
				</HStack>
			</VStack>
		</Box>
	);
}

type Props = {
	feilmelding: string | null;
	respons: BeregningResponse | null;
};

export function Innsendingstidslinje({ feilmelding, respons }: Props) {
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
	const [tidslinjeMåneder, settTidslinjeMåneder] = useState(6);
	const tidslinjeSteg = Math.floor(tidslinjeMåneder / 2);
	const kjeder = respons?.oppdragstidslinje.kjeder ?? [];
	const allePerioder = kjeder.flatMap((kjede) =>
		kjede.kjedeVersjoner.flatMap((versjon) => versjon.kjedePerioder),
	);
	const [tidslinjeStart, settTidslinjeStart] = useState<Date | null>(() =>
		tidligsteMåned(allePerioder),
	);
	const [forrigeRespons, settForrigeRespons] = useState(respons);
	if (respons !== forrigeRespons) {
		settForrigeRespons(respons);
		settValgtPeriode(null);
		settUtvidedeKjeder(new Set());
		settTidslinjeStart(tidligsteMåned(allePerioder));
	}
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

	return (
		<Box
			borderColor="neutral-subtle"
			borderWidth="1"
			borderRadius="12"
			background="info-moderateA"
			asChild
		>
			<VStack width="100%" padding="space-32" gap="space-16">
				<Heading id="tidslinje-heading" level="2" size="medium">
					Innsendingstidslinje
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
					<BodyShort>Ingen innsendingstidslinje å vise.</BodyShort>
				) : (
					tidslinjeStart &&
					tidslinjeSlutt && (
						<>
							<HStack justify="end" align="center" gap="space-8">
								<HStack gap="space-4" wrap={false}>
									<Box background="default" asChild>
										<Button
											type="button"
											data-color="neutral"
											variant="secondary"
											size="small"
											icon={<ChevronLeftIcon aria-hidden />}
											aria-label="Forrige periode"
											onClick={() => flyttTidslinje(-tidslinjeSteg)}
										/>
									</Box>
									<Box background="default" asChild>
										<Button
											type="button"
											data-color="neutral"
											variant="secondary"
											size="small"
											icon={<ChevronRightIcon aria-hidden />}
											aria-label="Neste periode"
											onClick={() => flyttTidslinje(tidslinjeSteg)}
										/>
									</Box>
								</HStack>
								<ToggleGroup
									aria-label="Antall måneder i perioden"
									data-color="neutral"
									size="small"
									value={String(tidslinjeMåneder)}
									onChange={(value) => settTidslinjeMåneder(Number(value))}
								>
									<ToggleGroup.Item value="3" label="3 måneder" />
									<ToggleGroup.Item value="6" label="6 måneder" />
									<ToggleGroup.Item value="12" label="12 måneder" />
								</ToggleGroup>
							</HStack>
							<Box
								background="default"
								padding="space-16"
								borderWidth="2"
								borderColor="neutral"
								borderRadius="12"
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
																		<span>
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
							background="default"
							padding="space-24"
							borderWidth="2"
							borderRadius="12"
							borderColor="neutral"
						>
							<Heading level="3" size="small">
								Detaljer for valgt periode
							</Heading>
							<HGrid
								columns="repeat(auto-fit, minmax(min(100%, 16rem), 1fr))"
								gap="space-12"
								marginBlock="space-20"
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
		</Box>
	);
}
