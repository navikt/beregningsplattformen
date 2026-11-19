import { BodyShort, Box, Heading, VStack } from "@navikt/ds-react";
import type { EkstraInfo } from "../types/BeregningResponse";
import styles from "./Beregningsresultater.module.css";

function formaterEkstraInfo(info: EkstraInfo): string {
	if (!info.template) return info.tekst ?? info.grunn;

	return info.template.replace(
		/\{([^{}:]+)(?::[^{}]+)?\}/g,
		(plassholder: string, navn: string) => {
			const verdi = info.verdier[navn];
			if (!verdi) return plassholder;
			return verdi.skjult ? "***SKJULT***" : verdi.verdi;
		},
	);
}

export function EkstraInfoDetaljer({
	ekstraInfo,
	tittel,
}: {
	ekstraInfo: readonly EkstraInfo[] | null;
	tittel?: string;
}) {
	if (!ekstraInfo?.length) return null;

	return (
		<Box
			background="neutral-soft"
			borderRadius="8"
			padding="space-12"
			paddingInline="space-24"
			minWidth="0"
			className={styles.beregningsresultater__ekstrainfo}
		>
			<VStack gap="space-8">
				{tittel && (
					<Heading level="4" size="small">
						{tittel}
					</Heading>
				)}
				{ekstraInfo.map((info, indeks) => (
					<BodyShort key={`${info.grunn}-${indeks}`} size="small">
						{formaterEkstraInfo(info)}
					</BodyShort>
				))}
			</VStack>
		</Box>
	);
}
