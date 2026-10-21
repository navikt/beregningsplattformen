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
import { templateFields } from "../util/template";
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
	const globaleFelter = felter.filter(
		(felt) => felt.oppdragslinje === undefined,
	);
	const oppdragslinjer = [
		...new Set(
			felter
				.map((felt) => felt.oppdragslinje)
				.filter((linje): linje is number => linje !== undefined),
		),
	];

	const labels: Record<string, string> = {
		datoVedtakFom: "Fra dato",
		datoVedtakTom: "Til dato",
		sats: "Sats",
		oppdragGjelderId: "Gjelder",
		utbetalesTilId: "Utbetales til",
		vedtakssats: "Vedtakssats",
	};

	const renderFelter = (gruppefelter: typeof felter) =>
		gruppefelter.map((felt) => {
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
					autoComplete={felt.kind === "pnr" ? "off" : undefined}
					value={value}
					readOnly={sender}
					onChange={(hendelse) => onChange(hendelse.target.value)}
				/>
			);
		});

	const renderFeltgruppe = (
		legend: string,
		gruppefelter: typeof felter,
		datoer = false,
	) => {
		if (gruppefelter.length === 0) return null;

		return (
			<Fieldset key={legend} legend={legend} hideLegend={datoer}>
				<HGrid
					columns={{
						xs: 1,
						sm: datoer ? "max-content max-content" : 1,
					}}
					gap="space-16"
				>
					{renderFelter(gruppefelter)}
				</HGrid>
			</Fieldset>
		);
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
				</Accordion>
				{globaleFelter.length > 0 && (
					<Fieldset
						legend={
							<Heading level="3" size="small">
								Oppdrag
							</Heading>
						}
					>
						<HGrid columns={{ xs: 1, md: 2 }} gap="space-16">
							{renderFelter(globaleFelter)}
						</HGrid>
					</Fieldset>
				)}
				{oppdragslinjer.map((linje) => {
					const linjefelter = felter.filter(
						(felt) => felt.oppdragslinje === linje,
					);

					return (
						<section
							key={linje}
							className={styles.skjema__oppdragslinje}
							aria-labelledby={`oppdragslinje-${nummer}-${linje}`}
						>
							<Heading
								id={`oppdragslinje-${nummer}-${linje}`}
								level="3"
								size="small"
							>
								Oppdragslinje {linje}
							</Heading>
							<HGrid columns={{ xs: 1, md: 2 }} gap="space-32" align="start">
								<div className={styles.skjema__feltkolonne}>
									{renderFeltgruppe(
										"Vedtaksperiode",
										linjefelter.filter((felt) => felt.kind === "dato"),
										true,
									)}
									{renderFelter(
										linjefelter.filter((felt) => felt.kind === "pnr"),
									)}
								</div>
								<div className={styles.skjema__feltkolonne}>
									{renderFeltgruppe(
										"Satser",
										linjefelter.filter((felt) => felt.kind === "number"),
									)}
								</div>
							</HGrid>
						</section>
					);
				})}
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
