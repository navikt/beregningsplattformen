import { ChevronDownIcon } from "@navikt/aksel-icons";
import {
	ActionMenu,
	Box,
	Button,
	HStack,
	Tabs,
	VStack,
} from "@navikt/ds-react";
import { useMemo, useState } from "react";
import styles from "./InnsendingerSkjema.module.css";
import { InnsendingKort } from "./InnsendingKort";
import {
	bygg,
	type InnsendingSkjema,
	kopierInnsending,
	nyInnsending,
} from "./skjema";

export function InnsendingerSkjema() {
	const [innsendinger, setInnsendinger] = useState<InnsendingSkjema[]>(() => [
		nyInnsending(),
	]);
	const [aapen, setAapen] = useState<string>();
	const resultat = useMemo(() => bygg(innsendinger), [innsendinger]);

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
		<VStack gap="space-24">
			<Tabs value={aapenNokkel ?? ""} onChange={setAapen}>
				<HStack gap="space-16" align="end" wrap={false}>
					<div className={styles.faner}>
						<Tabs.List>
							{innsendinger.map((innsending, index) => (
								<Tabs.Tab
									key={innsending.nokkel}
									value={innsending.nokkel}
									label={fanenavn(navn, index)}
								/>
							))}
						</Tabs.List>
					</div>
					<div>
						<ActionMenu>
							<ActionMenu.Trigger>
								<Button
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
					</div>
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
										? () =>
												setInnsendinger((forrige) =>
													forrige.filter((i) => i.nokkel !== innsending.nokkel),
												)
										: undefined
								}
							/>
						</Box>
					</Tabs.Panel>
				))}
			</Tabs>
		</VStack>
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
