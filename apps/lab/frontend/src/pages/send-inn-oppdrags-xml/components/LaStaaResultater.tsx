import {
	Box,
	Button,
	Checkbox,
	Dialog,
	ExpansionCard,
	HStack,
	Textarea,
} from "@navikt/ds-react";
import { useState } from "react";
import type { LagretResultat } from "../../../types/Beregningsskjema";
import { Beregningsresultater } from "./Beregningsresultater";

export function LaStaaResultater({
	gjeldende,
	lagret,
	onChange,
}: {
	gjeldende: LagretResultat["beregninger"];
	lagret: LagretResultat | undefined;
	onChange: (lagret: LagretResultat | undefined) => void;
}) {
	const [åpen, settÅpen] = useState(false);
	const [bekrefter, settBekrefter] = useState(false);
	// Samme array-referanse betyr at gjeldende resultat er det som står igjen.
	const gjeldendeErLagret = lagret?.beregninger === gjeldende;

	const laStå = () => {
		onChange({ beregninger: gjeldende, melding: "" });
		settÅpen(true);
	};

	return (
		<>
			{!gjeldendeErLagret && (gjeldende.length > 0 || !lagret) && (
				<Beregningsresultater
					beregninger={gjeldende}
					kontroller={
						<Checkbox
							checked={false}
							onChange={() => (lagret ? settBekrefter(true) : laStå())}
						>
							La stå
						</Checkbox>
					}
				/>
			)}
			{lagret && (
				<Box width="100%" asChild>
					<ExpansionCard
						aria-label="Resultat(La stå)"
						open={åpen}
						onToggle={settÅpen}
					>
						<ExpansionCard.Header>
							<HStack align="center" gap="space-16" wrap={false}>
								<ExpansionCard.Title as="div">
									Resultat(La stå)
								</ExpansionCard.Title>
								<Box
									style={{
										position: "relative",
										zIndex: 2,
										flexShrink: 0,
										whiteSpace: "nowrap",
									}}
								>
									<Checkbox checked onChange={() => onChange(undefined)}>
										La stå
									</Checkbox>
								</Box>
							</HStack>
						</ExpansionCard.Header>
						<Box padding="space-20">
							<Textarea
								label="Melding"
								value={lagret.melding}
								onChange={(event) =>
									onChange({ ...lagret, melding: event.target.value })
								}
							/>
						</Box>
						<ExpansionCard.Content>
							<Beregningsresultater beregninger={lagret.beregninger} />
						</ExpansionCard.Content>
					</ExpansionCard>
				</Box>
			)}
			<Dialog open={bekrefter} onOpenChange={settBekrefter}>
				<Dialog.Popup>
					<Dialog.Header>
						<Dialog.Title>Erstatte resultat som står igjen?</Dialog.Title>
					</Dialog.Header>
					<Dialog.Body>
						Det eksisterende resultatet og meldingen blir overskrevet.
					</Dialog.Body>
					<Dialog.Footer>
						<Dialog.CloseTrigger>
							<Button type="button" variant="secondary">
								Avbryt
							</Button>
						</Dialog.CloseTrigger>
						<Button
							type="button"
							variant="primary"
							data-color="danger"
							onClick={() => {
								laStå();
								settBekrefter(false);
							}}
						>
							Overskriv
						</Button>
					</Dialog.Footer>
				</Dialog.Popup>
			</Dialog>
		</>
	);
}
