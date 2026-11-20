import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	Accordion,
	Button,
	ExpansionCard,
	HStack,
	Tag,
	VStack,
} from "@navikt/ds-react";
import { useId, useState } from "react";
import {
	attestantfelter,
	gradfelter,
	linjefelter,
	linjetekstfelter,
	refusjonsinfofelter,
} from "./feltdefinisjoner";
import { useFokusVedVisning } from "./fokus";
import { FeltGruppe, ListeFelter } from "./Skjemafelter";
import { type KjedeSkjema, type LinjeSkjema, nyLinje } from "./skjema";

function LinjeKort({
	nummer,
	linje,
	fokuser,
	onEndring,
	onFjern,
}: {
	nummer: number;
	linje: LinjeSkjema;
	fokuser: boolean;
	onEndring: (linje: LinjeSkjema) => void;
	onFjern: () => void;
}) {
	const tittelId = useId();
	const ref = useFokusVedVisning<HTMLDivElement>(fokuser);

	return (
		<ExpansionCard
			ref={ref}
			size="small"
			defaultOpen
			aria-labelledby={tittelId}
		>
			<ExpansionCard.Header>
				<ExpansionCard.Title id={tittelId} as="h5" size="small">
					Linje {nummer}
				</ExpansionCard.Title>
			</ExpansionCard.Header>
			<ExpansionCard.Content>
				<VStack gap="space-12">
					<FeltGruppe
						legend={`Linje ${nummer}`}
						hideLegend
						definisjoner={linjefelter}
						verdier={linje.felter}
						onEndring={(felter) => onEndring({ ...linje, felter })}
					/>
					<Accordion size="small">
						<Accordion.Item>
							<Accordion.Header>refusjonsinfo-156</Accordion.Header>
							<Accordion.Content>
								<FeltGruppe
									legend="refusjonsinfo-156"
									hideLegend
									definisjoner={refusjonsinfofelter}
									verdier={linje.refusjonsinfo}
									onEndring={(refusjonsinfo) =>
										onEndring({ ...linje, refusjonsinfo })
									}
								/>
							</Accordion.Content>
						</Accordion.Item>
						<Accordion.Item>
							<Accordion.Header>
								linje-tekst-158 ({linje.linjeTekster.length})
							</Accordion.Header>
							<Accordion.Content>
								<ListeFelter
									tittel="Linjetekst"
									elementnavn="linjetekst"
									definisjoner={linjetekstfelter}
									elementer={linje.linjeTekster}
									onEndring={(linjeTekster) =>
										onEndring({ ...linje, linjeTekster })
									}
								/>
							</Accordion.Content>
						</Accordion.Item>
						<Accordion.Item>
							<Accordion.Header>
								grad-170 ({linje.grader.length})
							</Accordion.Header>
							<Accordion.Content>
								<ListeFelter
									tittel="Grad"
									elementnavn="grad"
									definisjoner={gradfelter}
									elementer={linje.grader}
									onEndring={(grader) => onEndring({ ...linje, grader })}
								/>
							</Accordion.Content>
						</Accordion.Item>
						<Accordion.Item>
							<Accordion.Header>
								attestant-180 ({linje.attestanter.length})
							</Accordion.Header>
							<Accordion.Content>
								<ListeFelter
									tittel="Attestant"
									elementnavn="attestant"
									definisjoner={attestantfelter}
									elementer={linje.attestanter}
									onEndring={(attestanter) =>
										onEndring({ ...linje, attestanter })
									}
								/>
							</Accordion.Content>
						</Accordion.Item>
					</Accordion>
					<HStack justify="end">
						<Button
							size="xsmall"
							variant="tertiary"
							icon={<TrashIcon aria-hidden />}
							onClick={onFjern}
						>
							Fjern linje {nummer}
						</Button>
					</HStack>
				</VStack>
			</ExpansionCard.Content>
		</ExpansionCard>
	);
}

export function KjedeKort({
	kjede,
	kjedeId,
	fokuser,
	onEndring,
	onFjern,
}: {
	kjede: KjedeSkjema;
	kjedeId?: number;
	fokuser: boolean;
	onEndring: (kjede: KjedeSkjema) => void;
	onFjern: () => void;
}) {
	const navn = `Kjede ${kjedeId ?? kjede.fortsetterKjedeId ?? "?"}`;
	const settLinjer = (linjer: LinjeSkjema[]) => onEndring({ ...kjede, linjer });
	const tittelId = useId();
	// En ny kjede har ingen linjer ennå, så fokus går til knappen for å legge til den første
	const nyLinjeRef = useFokusVedVisning<HTMLButtonElement>(fokuser);
	const [nyLinjeNokkel, setNyLinjeNokkel] = useState<string>();

	return (
		<ExpansionCard size="small" defaultOpen aria-labelledby={tittelId}>
			<ExpansionCard.Header>
				<HStack gap="space-8" align="center">
					<ExpansionCard.Title id={tittelId} as="h4" size="small">
						{navn}
					</ExpansionCard.Title>
					{kjede.fortsetterKjedeId != null && (
						<Tag size="small" variant="info">
							Fortsetter fra tidligere innsending
						</Tag>
					)}
				</HStack>
			</ExpansionCard.Header>
			<ExpansionCard.Content>
				<VStack gap="space-12">
					{kjede.linjer.map((linje, index) => (
						<LinjeKort
							key={linje.nokkel}
							nummer={index + 1}
							linje={linje}
							fokuser={linje.nokkel === nyLinjeNokkel}
							onEndring={(ny) =>
								settLinjer(kjede.linjer.map((l) => (l === linje ? ny : l)))
							}
							onFjern={() => {
								settLinjer(kjede.linjer.filter((l) => l !== linje));
								nyLinjeRef.current?.focus();
							}}
						/>
					))}
					<HStack justify="space-between" gap="space-8">
						<Button
							ref={nyLinjeRef}
							size="small"
							variant="secondary"
							icon={<PlusIcon aria-hidden />}
							onClick={() => {
								const linje = nyLinje();
								setNyLinjeNokkel(linje.nokkel);
								settLinjer([...kjede.linjer, linje]);
							}}
						>
							Ny linje
						</Button>
						<Button
							size="small"
							variant="tertiary"
							icon={<TrashIcon aria-hidden />}
							onClick={onFjern}
						>
							Fjern {navn.toLowerCase()}
						</Button>
					</HStack>
				</VStack>
			</ExpansionCard.Content>
		</ExpansionCard>
	);
}
