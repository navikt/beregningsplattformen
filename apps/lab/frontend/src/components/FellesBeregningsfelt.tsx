import {
	Box,
	ErrorSummary,
	Fieldset,
	Heading,
	HStack,
	VStack,
} from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { OppdragDatoFelt } from "./OppdragDatoFelt";

export interface FellesOpplysninger {
	fom: string;
	tom: string;
	fomGyldig: boolean;
	tomGyldig: boolean;
	beregningsTidspunkt?: string;
}

export interface FellesFeltfeil {
	felt: "fom" | "tom";
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
			padding="space-24"
			background="default"
			borderWidth="1"
			borderColor="neutral-subtle"
			borderRadius="12"
			aria-labelledby="felles-heading"
		>
			<VStack gap="space-16">
				<Heading id="felles-heading" level="2" size="medium">
					Beregningsopplysninger
				</Heading>
				{feil.length > 0 && (
					<ErrorSummary
						ref={oppsummering}
						heading="Rett feilene i beregningsopplysningene"
						headingTag="h3"
					>
						{feil.map(({ felt, melding }) => (
							<ErrorSummary.Item key={felt} href={`#felles-${felt}`}>
								{melding}
							</ErrorSummary.Item>
						))}
					</ErrorSummary>
				)}
				<Fieldset legend="Beregningsopplysninger" hideLegend disabled={sender} size="small">
					<HStack gap="space-16" align="start" wrap>
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
						<OppdragDatoFelt
							id="felles-beregningsTidspunkt"
							label="Beregningstidspunkt"
							value={verdi.beregningsTidspunkt ?? ""}
							readOnly={sender}
							onChange={(beregningsTidspunkt) =>
								onChange({ beregningsTidspunkt })
							}
						/>
					</HStack>
				</Fieldset>
			</VStack>
		</Box>
	);
}
