import { BodyShort, Button, LocalAlert, Modal } from "@navikt/ds-react";

interface Props {
	nullstilt: boolean;
	lagringsfeil: boolean;
	bekreftNullstilling: () => void;
}

export function SkjemalenkeMeldinger({
	nullstilt,
	lagringsfeil,
	bekreftNullstilling,
}: Props) {
	return (
		<>
			<Modal
				open={nullstilt}
				onClose={bekreftNullstilling}
				header={{ heading: "Skjemaet er nullstilt" }}
				width="small"
			>
				<Modal.Body>
					<BodyShort>
						Skjemadataene i lenken kunne ikke gjenopprettes. Skjemaet er
						nullstilt til standardverdiene.
					</BodyShort>
				</Modal.Body>
				<Modal.Footer>
					<Button type="button" onClick={bekreftNullstilling}>
						Lukk
					</Button>
				</Modal.Footer>
			</Modal>
			{lagringsfeil && (
				<LocalAlert status="warning">
					<LocalAlert.Header>
						<LocalAlert.Title>Lenken er ikke oppdatert</LocalAlert.Title>
					</LocalAlert.Header>
					<LocalAlert.Content>
						Skjemaet kunne ikke lagres i URL-en. Du kan fortsette å fylle ut,
						men lenken inneholder ikke de siste endringene.
					</LocalAlert.Content>
				</LocalAlert>
			)}
		</>
	);
}
