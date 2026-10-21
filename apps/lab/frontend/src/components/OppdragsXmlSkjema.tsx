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
import { useMemo, useState } from "react";
import { fillTypedTemplate, templateFields } from "../util/template";
import { OppdragDatoFelt } from "./OppdragDatoFelt";
import styles from "./OppdragsXmlSkjema.module.css";

interface OppdragsXmlSkjemaProps {
	nummer: number;
	xmlMal: string;
	feltverdier: Record<string, string>;
	xmlFeil: boolean;
	sender: boolean;
	onXmlMalChange: (xmlMal: string) => void;
	onFeltverdiChange: (key: string, value: string) => void;
	onNullstillFelter: () => void;
	onTilbakestillMal: () => void;
}

export function OppdragsXmlSkjema({
	nummer,
	xmlMal,
	feltverdier,
	xmlFeil,
	sender,
	onXmlMalChange,
	onFeltverdiChange,
	onNullstillFelter,
	onTilbakestillMal,
}: OppdragsXmlSkjemaProps) {
	const [xmlMalOpen, settXmlMalOpen] = useState(false);
	const felter = useMemo(() => templateFields(xmlMal), [xmlMal]);
	const generertXml = useMemo(
		() => fillTypedTemplate(xmlMal, felter, feltverdier),
		[xmlMal, felter, feltverdier],
	);

	const labels: Record<string, string> = {
		datoVedtakFom: "Fra dato",
		datoVedtakTom: "Til dato",
		sats: "Sats",
		oppdragGjelderId: "Gjelder",
		utbetalesTilId: "Utbetales til",
		vedtakssats: "Vedtakssats",
	};

	return (
		<div className={styles.skjema}>
			<Heading level="2" size="small">
				XML {nummer}
			</Heading>
			<div className={styles.skjema__form}>
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
									onXmlMalChange(hendelse.target.value);
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
													onFeltverdiChange(felt.key, nyVerdi);
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
					<Button
						type="button"
						variant="secondary"
						disabled={sender}
						onClick={onNullstillFelter}
					>
						Nullstill felter
					</Button>
					<Button
						type="button"
						variant="secondary"
						disabled={sender}
						onClick={onTilbakestillMal}
					>
						Tilbakestill standardmal
					</Button>
				</HStack>
			</div>
		</div>
	);
}
