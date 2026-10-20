import { ChevronDownIcon, ChevronRightIcon } from "@navikt/aksel-icons";
import {
	BodyShort,
	Box,
	Button,
	Heading,
	HStack,
	Textarea,
	Timeline,
} from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useState } from "react";
import tidslinjeData from "../../mock/tidslinje.json";
import { postTolkOppdrag } from "../api/apiService";
import { HttpStatusCodeError } from "../types/Error";
import styles from "./Lab.module.css";

export default function Laboratorium() {
	const [xmlTekst, settXmlTekst] = useState("");
	const [resultat, settResultat] = useState<string | null>(null);
	const [respons, settRespons] = useState<string | null>(null);
	const [sender, settSender] = useState(false);
	const [feltFeil, settFeltFeil] = useState<string | null>(null);
	const [visHistorikkKjede2, settVisHistorikkKjede2] = useState(false);
	const [valgtPeriode, settValgtPeriode] = useState<{
		id: string;
		melding: string;
		status: string;
	} | null>(null);

	const handterInnsending = async (hendelse: SubmitEvent<HTMLFormElement>) => {
		hendelse.preventDefault();
		if (sender) return;

		settResultat(null);
		settRespons(null);
		if (!xmlTekst.trim()) {
			settFeltFeil("Skriv inn et XML-oppdrag.");
			return;
		}
		settFeltFeil(null);
		settSender(true);

		try {
			settRespons(await postTolkOppdrag(xmlTekst));
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
				<Heading
					className={styles.laboratorium__overskrift}
					id="lab-heading"
					level="1"
					size="large"
				>
					Lab verktøy
				</Heading>
				<form
					className={styles.laboratorium__skjema}
					onSubmit={handterInnsending}
					aria-busy={sender}
				>
					<Textarea
						className={styles.laboratorium__xmlfelt}
						label="XML-oppdrag"
						description="Bruk bare syntetiske XML-data. Ikke lim inn personopplysninger eller reelle ID-er."
						minRows={16}
						readOnly={sender}
						error={feltFeil}
						value={xmlTekst}
						onChange={(hendelse) => {
							settXmlTekst(hendelse.target.value);
							settResultat(null);
							settFeltFeil(null);
						}}
					/>
					<Button type="submit" loading={sender}>
						Behandle XML
					</Button>
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
