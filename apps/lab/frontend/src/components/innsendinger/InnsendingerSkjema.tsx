import { ChevronDownIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	ActionMenu,
	Box,
	Button,
	Dialog,
	ExpansionCard,
	HStack,
	Tabs,
	VStack,
} from "@navikt/ds-react";
import { useId, useRef, useState } from "react";
import { useInnsendinger } from "./InnsendingerContext";
import { InnsendingKort } from "./InnsendingKort";
import {
	type InnsendingSkjema,
	kopierInnsending,
	nyInnsending,
} from "./skjema";

export function InnsendingerSkjema() {
	const { innsendinger, setInnsendinger, resultat, nullstillSkjema } =
		useInnsendinger();
	const [aapen, setAapen] = useState<string>();
	const nyInnsendingRef = useRef<HTMLButtonElement>(null);
	const tittelId = useId();

	// Faller tilbake til siste innsending når den åpne er fjernet
	const aapenNokkel = innsendinger.some((i) => i.nokkel === aapen)
		? aapen
		: innsendinger.at(-1)?.nokkel;

	const leggTil = (innsending: InnsendingSkjema) => {
		setInnsendinger((forrige) => [...forrige, innsending]);
		setAapen(innsending.nokkel);
	};

	const navn = innsendinger.map((innsending, index) =>
		oppdragsnavn(
			innsending.felter.kodeFagomraade,
			resultat.innsendinger[index].fagsystemId,
		),
	);

	// Eksisterende oppdrag, hver med indeksen til siste innsending på oppdraget
	const eksisterendeOppdrag = resultat.oppdragsforslag.map((oppdrag) => ({
		oppdragId: oppdrag.id,
		index: resultat.innsendinger.findLastIndex(
			(r) => r.oppdragId === oppdrag.id,
		),
	}));

	const fortsettOppdrag = (oppdragId: number, index: number) =>
		leggTil(
			kopierInnsending(
				innsendinger[index],
				oppdragId,
				resultat.innsendinger[index].kjedeIder,
			),
		);

	return (
		<ExpansionCard size="small" defaultOpen aria-labelledby={tittelId}>
			<ExpansionCard.Header>
				<ExpansionCard.Title id={tittelId} as="h2" size="medium">
					Innsendinger
				</ExpansionCard.Title>
				<ExpansionCard.Description>
					{innsendinger.length === 1
						? "1 innsending"
						: `${innsendinger.length} innsendinger`}
				</ExpansionCard.Description>
			</ExpansionCard.Header>
			<ExpansionCard.Content>
				<VStack gap="space-24" position="relative">
					<Tabs value={aapenNokkel ?? ""} onChange={setAapen}>
						<HStack gap="space-12" align="end" wrap={false}>
							{/* Lar fanelista krympe ved siden av knappen, så Aksel kan scrolle fanene */}
							<Box flexGrow="1" minWidth="0">
								<Tabs.List>
									{innsendinger.map((innsending, index) => (
										<Tabs.Tab
											key={innsending.nokkel}
											value={innsending.nokkel}
											label={fanenavn(navn, index)}
										/>
									))}
								</Tabs.List>
							</Box>
							<Dialog>
								<Dialog.Trigger>
									<Button
										type="button"
										variant="tertiary"
										data-color="danger"
										icon={<TrashIcon aria-hidden />}
									>
										Slett alt
									</Button>
								</Dialog.Trigger>
								<Dialog.Popup>
									<Dialog.Header>
										<Dialog.Title>Bekreft sletting</Dialog.Title>
									</Dialog.Header>
									<Dialog.Body>
										Er du sikker på at du vil slette alle innsendinger?
									</Dialog.Body>
									<Dialog.Footer>
										<Dialog.CloseTrigger>
											<Button variant="secondary">Avbryt</Button>
										</Dialog.CloseTrigger>
										<Button variant="danger" onClick={nullstillSkjema}>
											Slett alt
										</Button>
									</Dialog.Footer>
								</Dialog.Popup>
							</Dialog>
							<Box flexShrink="0">
								<ActionMenu>
									<ActionMenu.Trigger>
										<Button
											ref={nyInnsendingRef}
											variant="primary"
											icon={<ChevronDownIcon aria-hidden />}
											iconPosition="right"
										>
											Ny innsending
										</Button>
									</ActionMenu.Trigger>
									<ActionMenu.Content>
										<ActionMenu.Item onSelect={() => leggTil(nyInnsending())}>
											Nytt oppdrag
										</ActionMenu.Item>
										{resultat.oppdragsforslag.length > 0 && (
											<>
												<ActionMenu.Divider />
												<ActionMenu.Group label="Eksisterende oppdrag">
													{eksisterendeOppdrag.map(({ oppdragId, index }) => (
														<ActionMenu.Item
															key={oppdragId}
															onSelect={() => fortsettOppdrag(oppdragId, index)}
														>
															{navn[index]}
														</ActionMenu.Item>
													))}
												</ActionMenu.Group>
											</>
										)}
									</ActionMenu.Content>
								</ActionMenu>
							</Box>
						</HStack>
						{innsendinger.map((innsending, index) => (
							<Tabs.Panel key={innsending.nokkel} value={innsending.nokkel}>
								<Box paddingBlock="space-16 space-0">
									<InnsendingKort
										nummer={index + 1}
										innsending={innsending}
										resultat={resultat.innsendinger[index]}
										onEndring={(ny) =>
											setInnsendinger((forrige) =>
												forrige.map((i) => (i.nokkel === ny.nokkel ? ny : i)),
											)
										}
										onFjern={
											innsendinger.length > 1
												? () => {
														setInnsendinger((forrige) =>
															forrige.filter(
																(i) => i.nokkel !== innsending.nokkel,
															),
														);
														// Knappen forsvinner med fanen, så fokus må flyttes til noe som blir stående
														nyInnsendingRef.current?.focus();
													}
												: undefined
										}
									/>
								</Box>
							</Tabs.Panel>
						))}
					</Tabs>
				</VStack>
			</ExpansionCard.Content>
		</ExpansionCard>
	);
}

const oppdragsnavn = (fagomraade?: string, fagsystemId?: string) =>
	`${fagomraade || "(uten fagområde)"} / ${fagsystemId ?? "(ukjent oppdrag)"}`;

// Innsendinger med samme navn får løpenummer fra og med nummer to
function fanenavn(navn: string[], index: number): string {
	const nummer = navn
		.slice(0, index + 1)
		.filter((n) => n === navn[index]).length;
	return nummer > 1 ? `${navn[index]} (${nummer})` : navn[index];
}
