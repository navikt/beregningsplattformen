import { ChevronDownIcon, ChevronRightIcon } from "@navikt/aksel-icons";
import {
	Accordion,
	BodyShort,
	Box,
	Button,
	Fieldset,
	Heading,
	HGrid,
	HStack,
	LocalAlert,
	Textarea,
	TextField,
	Timeline,
} from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useMemo, useState } from "react";
import tidslinjeData from "../../mock/tidslinje.json";
import { postTolkOppdrag } from "../api/apiService";
import { OppdragDatoFelt } from "../components/OppdragDatoFelt";
import { oppdragsXmlTypedTemplate } from "../data/oppdragsXmlTypedTemplate";
import { HttpStatusCodeError } from "../types/Error";
import { fillTypedTemplate, templateFields } from "../util/template";
import styles from "./SendInnOppdragsXML.module.css";

export default function SendInnOppdragsXML() {
	const [xmlMal, settXmlMal] = useState(oppdragsXmlTypedTemplate);
	const [xmlMalOpen, settXmlMalOpen] = useState(false);
	const [xmlFeil, settXmlFeil] = useState(false);
	const [feltverdier, settFeltverdier] = useState<Record<string, string>>({});
	const [resultat, settResultat] = useState<string | null>(null);
	const [respons, settRespons] = useState<string | null>(null);
	const [sender, settSender] = useState(false);
	const [visHistorikkKjede2, settVisHistorikkKjede2] = useState(false);
	const [valgtPeriode, settValgtPeriode] = useState<{
		id: string;
		melding: string;
		status: string;
	} | null>(null);
	const felter = useMemo(() => templateFields(xmlMal), [xmlMal]);
	const generertXml = useMemo(
		() => fillTypedTemplate(xmlMal, felter, feltverdier),
		[xmlMal, felter, feltverdier],
	);
	const fjernResultat = () => {
		settResultat(null);
		settRespons(null);
	};

	const labels: Record<string, string> = {
		datoVedtakFom: "Fra dato",
		datoVedtakTom: "Til dato",
		sats: "Sats",
		oppdragGjelderId: "Gjelder",
		utbetalesTilId: "Utbetales til",
		vedtakssats: "Vedtakssats",
	};

	const handterInnsending = async (hendelse: SubmitEvent<HTMLFormElement>) => {
		hendelse.preventDefault();
		if (sender) return;

		settResultat(null);
		settRespons(null);
		if (!generertXml.trim()) {
			settXmlFeil(true);
			settResultat("Skriv inn et XML-oppdrag.");
			return;
		}
		settXmlFeil(false);
		settSender(true);

		try {
			settRespons(await postTolkOppdrag(generertXml));
			settResultat("XML-en er behandlet.");
		} catch (error) {
			if (isAxiosError<string>(error) && error.response) {
				settRespons(error.response.data);
			}
			settResultat(
				error instanceof HttpStatusCodeError ||
					(isAxiosError(error) && error.response)
					? "Kunne ikke behandle XML-en. Kontroller innholdet og prøv igjen."
					: "Kunne ikke behandle XML-en. Prøv igjen.",
			);
		} finally {
			settSender(false);
		}
	};

	return (
		<main className={styles.laboratorium}>
			<section
				className={styles.laboratorium__panel}
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
				<form
					className={styles.laboratorium__skjema}
					onSubmit={handterInnsending}
					aria-busy={sender}
				>
					<Accordion>
						<Accordion.Item
							open={xmlMalOpen || xmlFeil}
							onOpenChange={settXmlMalOpen}
						>
							<Accordion.Header>XML-mal</Accordion.Header>
							<Accordion.Content>
								<Textarea
									className={styles.laboratorium__xmlfelt}
									label="XML-mal"
									error={xmlFeil ? "Skriv inn et XML-oppdrag." : undefined}
									description="Malmarkører i klammeparenteser blir erstattet med verdiene fra feltene under."
									minRows={16}
									readOnly={sender}
									value={xmlMal}
									onChange={(hendelse) => {
										const nyMal = hendelse.target.value;
										fjernResultat();
										settXmlFeil(false);
										settXmlMal(nyMal);
										const nyeFelter = templateFields(nyMal);
										settFeltverdier((forrige) =>
											Object.fromEntries(
												nyeFelter.map((felt) => [
													felt.key,
													forrige[felt.key] ?? felt.value,
												]),
											),
										);
									}}
								/>
							</Accordion.Content>
						</Accordion.Item>
						<Accordion.Item>
							<Accordion.Header>Forhåndsvis XML</Accordion.Header>
							<Accordion.Content>
								<pre className={styles.laboratorium__xmlforhandsvisning}>
									{generertXml}
								</pre>
							</Accordion.Content>
						</Accordion.Item>
					</Accordion>
					<HGrid columns={{ xs: 1, md: 2 }} gap="space-32" align="start">
						{[
							[
								{ kind: "pnr", legend: "Oppdrag", name: "oppdragGjelderId" },
								{ kind: "dato", legend: "Vedtaksperiode", name: undefined },
								{ kind: "pnr", legend: "Mottaker", name: "utbetalesTilId" },
							],
							[{ kind: "number", legend: "Satser", name: undefined }],
						].map((grupper) => (
							<div
								key={grupper[0].legend}
								className={styles.laboratorium__feltkolonne}
							>
								<Heading level="2" size="small">
									{grupper[0].kind === "number" ? "Satser" : "Oppdragsdetaljer"}
								</Heading>
								{grupper.map(({ kind, legend, name }) => {
									const gruppefelter = felter.filter(
										(felt) =>
											felt.kind === kind && (!name || felt.name === name),
									);
									if (gruppefelter.length === 0) return null;

									return (
										<Fieldset
											key={legend}
											legend={legend}
											hideLegend={kind !== "dato"}
										>
											<HGrid
												columns={{
													xs: 1,
													sm: kind === "dato" ? "max-content max-content" : 1,
												}}
												gap="space-16"
											>
												{gruppefelter.map((felt) => {
													const label = labels[felt.name] ?? felt.name;
													const value = feltverdier[felt.key] ?? felt.value;
													const onChange = (nyVerdi: string) => {
														fjernResultat();
														settXmlFeil(false);
														settFeltverdier((forrige) => ({
															...forrige,
															[felt.key]: nyVerdi,
														}));
													};

													if (felt.kind === "dato") {
														return (
															<OppdragDatoFelt
																key={felt.key}
																label={label}
																value={value}
																readOnly={sender}
																onChange={onChange}
															/>
														);
													}

													return (
														<TextField
															key={felt.key}
															label={label}
															type={felt.kind === "number" ? "number" : "text"}
															autoComplete={
																felt.kind === "pnr" ? "off" : undefined
															}
															value={value}
															readOnly={sender}
															onChange={(hendelse) =>
																onChange(hendelse.target.value)
															}
														/>
													);
												})}
											</HGrid>
										</Fieldset>
									);
								})}
							</div>
						))}
					</HGrid>
					<HStack
						className={styles.laboratorium__handlinger}
						gap="space-8"
						wrap
					>
						<Button type="submit" loading={sender} disabled={sender}>
							Behandle XML
						</Button>
						<Button
							type="button"
							variant="secondary"
							disabled={sender}
							onClick={() => {
								fjernResultat();
								settXmlFeil(false);
								settFeltverdier({});
							}}
						>
							Nullstill felter
						</Button>
						<Button
							type="button"
							variant="secondary"
							disabled={sender}
							onClick={() => {
								fjernResultat();
								settXmlFeil(false);
								settXmlMal(oppdragsXmlTypedTemplate);
								settFeltverdier({});
							}}
						>
							Tilbakestill standardmal
						</Button>
					</HStack>
					{resultat && (
						<p className={styles.laboratorium__status} role="status">
							{resultat}
						</p>
					)}
				</form>
				<section
					className={styles.laboratorium__tidslinje}
					aria-labelledby="tidslinje-heading"
				>
					<Heading id="tidslinje-heading" level="2" size="medium">
						Tidslinjer
					</Heading>
					<Timeline>
						{Object.entries(tidslinjeData.tidslinjer).map(
							([kjedeId, innslag]) => {
								const gjeldende = innslag[0];
								if (!gjeldende) return null;

								const harHistorikk = kjedeId === "2" && innslag.length > 1;

								return (
									<Timeline.Row
										key={kjedeId}
										className={
											harHistorikk && visHistorikkKjede2
												? styles["laboratorium__historikkgruppe-topp"]
												: undefined
										}
										label={
											<HStack
												className={styles.laboratorium__kjedeetikett}
												gap="space-8"
												align="center"
												wrap={false}
											>
												{harHistorikk ? (
													<Button
														className={styles.laboratorium__utvidknapp}
														variant="tertiary"
														data-color="neutral"
														size="small"
														icon={
															visHistorikkKjede2 ? (
																<ChevronDownIcon aria-hidden />
															) : (
																<ChevronRightIcon aria-hidden />
															)
														}
														aria-label={
															visHistorikkKjede2
																? "Skjul historiske perioder for kjede 2"
																: "Vis historiske perioder for kjede 2"
														}
														aria-expanded={visHistorikkKjede2}
														aria-controls={
															visHistorikkKjede2
																? "historikk-kjede-2"
																: undefined
														}
														onClick={() => {
															settVisHistorikkKjede2((vis) => !vis);
															if (valgtPeriode?.id.startsWith("historisk-")) {
																settValgtPeriode(null);
															}
														}}
													/>
												) : (
													<span
														className={styles.laboratorium__pilplass}
														aria-hidden="true"
													/>
												)}
												<BodyShort as="h3" size="small" weight="semibold">
													Kjede {kjedeId}
												</BodyShort>
											</HStack>
										}
									>
										<Timeline.Period
											id={`periode-${kjedeId}`}
											aria-controls="periodedetaljer"
											isActive={valgtPeriode?.id === `periode-${kjedeId}`}
											onSelectPeriod={() =>
												settValgtPeriode({
													id: `periode-${kjedeId}`,
													melding: gjeldende.melding,
													status: gjeldende.status,
												})
											}
											start={new Date(gjeldende.fomDato)}
											end={
												gjeldende.tomDato
													? new Date(gjeldende.tomDato)
													: new Date()
											}
											status="info"
											statusLabel="Nåværende situasjon"
										>
											<p>Fom dato: {gjeldende.fomDato}</p>
											<p>Tom dato: {gjeldende.tomDato ?? "Ingen sluttdato"}</p>
											<p>Navn: {gjeldende.mottaker}</p>
										</Timeline.Period>
									</Timeline.Row>
								);
							},
						)}
						{visHistorikkKjede2 &&
							tidslinjeData.tidslinjer["2"].slice(1).length > 0 && (
								<Timeline.Row
									id="historikk-kjede-2"
									className={styles["laboratorium__historikkgruppe-bunn"]}
									label={
										<span className="aksel-sr-only">
											Historiske perioder i kjede 2
										</span>
									}
								>
									{tidslinjeData.tidslinjer["2"].slice(1).map((periode) => (
										<Timeline.Period
											key={`historikk-${periode.fomDato}`}
											id={`historisk-${periode.fomDato}`}
											aria-controls="periodedetaljer"
											isActive={
												valgtPeriode?.id === `historisk-${periode.fomDato}`
											}
											onSelectPeriod={() =>
												settValgtPeriode({
													id: `historisk-${periode.fomDato}`,
													melding: periode.melding,
													status: periode.status,
												})
											}
											start={new Date(periode.fomDato)}
											end={
												periode.tomDato ? new Date(periode.tomDato) : new Date()
											}
											status="neutral"
											statusLabel="Historisk situasjon"
										>
											<p>Fom dato: {periode.fomDato}</p>
											<p>Tom dato: {periode.tomDato ?? "Ingen sluttdato"}</p>
											<p>Navn: {periode.mottaker}</p>
										</Timeline.Period>
									))}
								</Timeline.Row>
							)}
					</Timeline>
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
								<p>Melding: {valgtPeriode.melding}</p>
								<p>Status: {valgtPeriode.status}</p>
							</Box>
						)}
					</Box>
				</section>
				{respons !== null && (
					<section aria-labelledby="respons-heading">
						<h2 id="respons-heading">Respons fra backend</h2>
						<pre className={styles.laboratorium__respons}>{respons}</pre>
					</section>
				)}
			</section>
		</main>
	);
}
