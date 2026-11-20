import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	Accordion,
	Box,
	Button,
	Heading,
	HStack,
	Tag,
	VStack,
} from "@navikt/ds-react";
import {
	attestantfelter,
	gradfelter,
	linjefelter,
	linjetekstfelter,
	refusjonsinfofelter,
} from "./feltdefinisjoner";
import { FeltGruppe, ListeFelter } from "./Skjemafelter";
import { type KjedeSkjema, type LinjeSkjema, nyLinje } from "./skjema";

function LinjeKort({
	nummer,
	linje,
	onEndring,
	onFjern,
}: {
	nummer: number;
	linje: LinjeSkjema;
	onEndring: (linje: LinjeSkjema) => void;
	onFjern: () => void;
}) {
	return (
		<Box
			as="section"
			borderWidth="1"
			borderColor="neutral"
			borderRadius="8"
			padding="space-16"
		>
			<VStack gap="space-12">
				<HStack justify="space-between" align="center">
					<Heading level="4" size="xsmall">
						Linje {nummer}
					</Heading>
					<Button
						size="xsmall"
						variant="tertiary"
						icon={<TrashIcon aria-hidden />}
						onClick={onFjern}
					>
						Fjern linje
					</Button>
				</HStack>
				<FeltGruppe
					definisjoner={linjefelter}
					verdier={linje.felter}
					onEndring={(felter) => onEndring({ ...linje, felter })}
				/>
				<Accordion size="small">
					<Accordion.Item>
						<Accordion.Header>refusjonsinfo-156</Accordion.Header>
						<Accordion.Content>
							<FeltGruppe
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
			</VStack>
		</Box>
	);
}

export function KjedeKort({
	kjede,
	kjedeId,
	onEndring,
	onFjern,
}: {
	kjede: KjedeSkjema;
	kjedeId?: number;
	onEndring: (kjede: KjedeSkjema) => void;
	onFjern: () => void;
}) {
	const navn = `Kjede ${kjedeId ?? kjede.fortsetterKjedeId ?? "?"}`;
	const settLinjer = (linjer: LinjeSkjema[]) => onEndring({ ...kjede, linjer });

	return (
		<Box
			as="section"
			borderWidth="1"
			borderColor="neutral"
			borderRadius="12"
			padding="space-16"
		>
			<VStack gap="space-12">
				<HStack justify="space-between" align="center" gap="space-8">
					<HStack gap="space-8" align="center">
						<Heading level="3" size="small">
							{navn}
						</Heading>
						{kjede.fortsetterKjedeId != null && (
							<Tag size="small" variant="info">
								Fortsetter fra tidligere innsending
							</Tag>
						)}
					</HStack>
					<Button
						size="xsmall"
						variant="tertiary"
						icon={<TrashIcon aria-hidden />}
						onClick={onFjern}
					>
						Fjern kjede
					</Button>
				</HStack>
				{kjede.linjer.map((linje, index) => (
					<LinjeKort
						key={linje.nokkel}
						nummer={index + 1}
						linje={linje}
						onEndring={(ny) =>
							settLinjer(kjede.linjer.map((l) => (l === linje ? ny : l)))
						}
						onFjern={() => settLinjer(kjede.linjer.filter((l) => l !== linje))}
					/>
				))}
				<div>
					<Button
						size="small"
						variant="secondary"
						icon={<PlusIcon aria-hidden />}
						onClick={() => settLinjer([...kjede.linjer, nyLinje()])}
					>
						Ny linje
					</Button>
				</div>
			</VStack>
		</Box>
	);
}
