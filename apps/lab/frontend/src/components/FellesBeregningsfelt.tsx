import {
	BodyShort,
	Box,
	ErrorSummary,
	Fieldset,
	Heading,
	HGrid,
	TextField,
	VStack,
} from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { OppdragDatoFelt } from "./OppdragDatoFelt";

export interface FellesOpplysninger {
	gjelderId: string;
	fom: string;
	tom: string;
	fomGyldig: boolean;
	tomGyldig: boolean;
}

export interface FellesFeltfeil {
	felt: "gjelderId" | "fom" | "tom";
	melding: string;
}

interface Props {
	verdi: FellesOpplysninger;
	feil: FellesFeltfeil[];
	sender: boolean;
	onChange: (endringer: Partial<FellesOpplysninger>) => void;
}

export function FellesBeregningsfelt({ verdi, feil, sender, onChange }: Props) {
	const oppsummering = useRef<HTMLDivElement>(null);
	useEffect(() => {
		if (feil.length > 0) oppsummering.current?.focus();
	}, [feil]);
	const feltfeil = (felt: FellesFeltfeil["felt"]) =>
		feil.find((feil) => feil.felt === felt)?.melding;

	return (
		<Box
			as="section"
			marginBlock="space-0 space-24"
			aria-labelledby="felles-heading"
		>
			<VStack gap="space-16">
				<Heading id="felles-heading" level="2" size="small">
					Felles opplysninger
				</Heading>
				<BodyShort>
					Gjelder alle oppdrag på siden. Bruk en syntetisk gjelder-ID med 11 sifre.
					Tomme datoer utledes fra oppdragene.
				</BodyShort>
				{feil.length > 0 && (
					<ErrorSummary
						ref={oppsummering}
						heading="Rett feilene i fellesopplysningene"
						headingTag="h3"
					>
						{feil.map(({ felt, melding }) => (
							<ErrorSummary.Item key={felt} href={`#felles-${felt}`}>
								{melding}
							</ErrorSummary.Item>
						))}
					</ErrorSummary>
				)}
				<Fieldset legend="Felles opplysninger" hideLegend disabled={sender}>
					<HGrid columns={{ xs: 1, lg: 3 }} gap="space-16" align="start">
						<TextField
							id="felles-gjelderId"
							label="Gjelder-ID"
							autoComplete="off"
							inputMode="numeric"
							value={verdi.gjelderId}
							readOnly={sender}
							error={feltfeil("gjelderId")}
							onChange={(event) => onChange({ gjelderId: event.target.value })}
						/>
						<OppdragDatoFelt
							id="felles-fom"
							label="Beregning fra"
							value={verdi.fom}
							readOnly={sender}
							error={feltfeil("fom")}
							onChange={(fom) => onChange({ fom })}
							onValidityChange={(fomGyldig) => onChange({ fomGyldig })}
						/>
						<OppdragDatoFelt
							id="felles-tom"
							label="Beregning til"
							value={verdi.tom}
							readOnly={sender}
							error={feltfeil("tom")}
							onChange={(tom) => onChange({ tom })}
							onValidityChange={(tomGyldig) => onChange({ tomGyldig })}
						/>
					</HGrid>
				</Fieldset>
			</VStack>
		</Box>
	);
}
