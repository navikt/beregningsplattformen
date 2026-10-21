import {
	Accordion,
	Button,
	Fieldset,
	Heading,
	HGrid,
	HStack,
	Textarea,
	TextField,
} from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useId, useMemo, useState } from "react";
import { postTolkOppdrag } from "../api/apiService";
import { oppdragsXmlTypedTemplate } from "../data/oppdragsXmlTypedTemplate";
import { HttpStatusCodeError } from "../types/Error";
import { fillTypedTemplate, templateFields } from "../util/template";
import { OppdragDatoFelt } from "./OppdragDatoFelt";
import styles from "./OppdragsXmlSkjema.module.css";

interface OppdragsXmlSkjemaProps {
	nummer: number;
}

export function OppdragsXmlSkjema({ nummer }: OppdragsXmlSkjemaProps) {
	const [xmlMal, settXmlMal] = useState(oppdragsXmlTypedTemplate);
	const [xmlMalOpen, settXmlMalOpen] = useState(false);
	const [xmlFeil, settXmlFeil] = useState(false);
	const [feltverdier, settFeltverdier] = useState<Record<string, string>>({});
	const [resultat, settResultat] = useState<string | null>(null);
	const [respons, settRespons] = useState<string | null>(null);
	const [sender, settSender] = useState(false);
	const responsHeadingId = useId();
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
		<div className={styles.skjema}>
			<Heading level="2" size="small">
				XML {nummer}
			</Heading>
			<form
				className={styles.skjema__form}
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
								className={styles.skjema__xmlfelt}
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
							<pre className={styles.skjema__xmlforhandsvisning}>
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
						<div key={grupper[0].legend} className={styles.skjema__feltkolonne}>
							<Heading level="3" size="small">
								{grupper[0].kind === "number" ? "Satser" : "Oppdragsdetaljer"}
							</Heading>
							{grupper.map(({ kind, legend, name }) => {
								const gruppefelter = felter.filter(
									(felt) => felt.kind === kind && (!name || felt.name === name),
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
				<HStack className={styles.skjema__handlinger} gap="space-8" wrap>
					<Button type="submit" loading={sender} disabled={sender}>
						Behandle XML {nummer}
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
					<p className={styles.skjema__status} role="status">
						{resultat}
					</p>
				)}
			</form>
			{respons !== null && (
				<section aria-labelledby={responsHeadingId}>
					<h2 id={responsHeadingId}>Respons for XML {nummer}</h2>
					<pre className={styles.skjema__respons}>{respons}</pre>
				</section>
			)}
		</div>
	);
}
