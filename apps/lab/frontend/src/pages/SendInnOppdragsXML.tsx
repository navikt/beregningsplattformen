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
import { useRef, useState } from "react";
import tidslinjeData from "../../mock/tidslinje.json";
import { OppdragsXmlSkjema } from "../components/OppdragsXmlSkjema";
import styles from "./SendInnOppdragsXML.module.css";

export default function SendInnOppdragsXML() {
	const [xmlUtkast, settXmlUtkast] = useState([1]);
	const [valgtXmlId, settValgtXmlId] = useState(1);
	const nesteXmlId = useRef(2);
	const [utvidedeKjeder, settUtvidedeKjeder] = useState<Set<string>>(
		() => new Set(),
	);
	const [valgtPeriode, settValgtPeriode] = useState<{
		id: string;
		kjedeId: string;
		versjonsindeks: number;
		periodeindeks: number;
		fraDato: string;
		tilDato: string | null;
		utbetalesTil: string;
		historisk: boolean;
	} | null>(null);
	const [tidslinjeMåneder, settTidslinjeMåneder] = useState(4);
	const allePerioder = tidslinjeData.kjeder.flatMap((kjede) =>
		kjede.kjedeVersjoner.flatMap((versjon) => versjon.kjedePerioder),
	);
	const [tidslinjeStart, settTidslinjeStart] = useState<Date | null>(() => {
		if (allePerioder.length === 0) return null;
		const tidligsteDato = new Date(
			Math.min(
				...allePerioder.map((periode) => new Date(periode.fraDato).getTime()),
			),
		);
		return new Date(tidligsteDato.getFullYear(), tidligsteDato.getMonth(), 1);
	});
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
				<fieldset className={styles.laboratorium__xmlvalg}>
					<legend className="aksel-sr-only">Velg XML-utkast</legend>
					{xmlUtkast.map((xmlId) => (
						<Button
							key={xmlId}
							type="button"
							variant={valgtXmlId === xmlId ? "primary" : "secondary"}
							aria-pressed={valgtXmlId === xmlId}
							onClick={() => settValgtXmlId(xmlId)}
						>
							XML {xmlId}
						</Button>
					))}
				</fieldset>
				{xmlUtkast.map((xmlId) => (
					<div key={xmlId} hidden={valgtXmlId !== xmlId}>
						<OppdragsXmlSkjema nummer={xmlId} />
					</div>
				))}
				<div className={styles.laboratorium__leggtil}>
					<Button
						type="button"
						variant="secondary"
						icon={<PlusIcon aria-hidden />}
						onClick={() => {
							const nyttXmlId = nesteXmlId.current;
							nesteXmlId.current += 1;
							settXmlUtkast((utkast) => [...utkast, nyttXmlId]);
							settValgtXmlId(nyttXmlId);
						}}
					>
						Legg til XML
					</Button>
				</div>
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
								{tidslinjeData.kjeder.flatMap((kjede, kjedeindeks) => {
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
													settValgtPeriode({
														id: periodeId,
														kjedeId: kjede.id,
														versjonsindeks,
														periodeindeks,
														fraDato: periode.fraDato,
														tilDato: periode.tilDato,
														utbetalesTil: periode.utbetalesTil,
														historisk,
													})
												}
												start={new Date(periode.fraDato)}
												end={
													periode.tilDato
														? new Date(periode.tilDato)
														: new Date()
												}
												status={historisk ? "warning" : "success"}
											>
												<p>Fra dato: {periode.fraDato}</p>
												<p>Til dato: {periode.tilDato ?? "Ingen sluttdato"}</p>
												<p>Utbetales til: {periode.utbetalesTil}</p>
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
						<Box background="neutral-soft" padding="space-16">
							<Heading level="3" size="small">
								Detaljer for valgt periode
							</Heading>
							<p>Kjede-ID: {valgtPeriode.kjedeId}</p>
							<p>Fra dato: {valgtPeriode.fraDato}</p>
							<p>Til dato: {valgtPeriode.tilDato ?? "Ingen sluttdato"}</p>
							<p>Utbetales til: {valgtPeriode.utbetalesTil}</p>
						</Box>
					)}
				</Box>
			</section>
		</main>
	);
}
