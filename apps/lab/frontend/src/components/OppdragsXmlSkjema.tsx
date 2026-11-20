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
	const felter = useMemo(
		() => templateFields(xmlMal).filter((felt) => felt.name !== "oppdragGjelderId"),
		[xmlMal],
	);
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

	return <div className={styles.skjema}></div>;
}
