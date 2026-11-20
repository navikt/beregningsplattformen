import { ChevronDownIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	Accordion,
	ActionMenu,
	Box,
	Button,
	Heading,
	HGrid,
	HStack,
	InlineMessage,
	TextField,
	VStack,
} from "@navikt/ds-react";
import {
	avstemmingsfelter,
	avventfelter,
	enhetsfelter,
	omposteringsfelter,
	oppdragsfelter,
	tekstfelter,
} from "./feltdefinisjoner";
import { formaterXml } from "./formaterXml";
import styles from "./InnsendingerSkjema.module.css";
import { KjedeKort } from "./KjedeKort";
import { FeltGruppe, ListeFelter } from "./Skjemafelter";
import {
	type InnsendingResultat,
	type InnsendingSkjema,
	type KjedeSkjema,
	nyKjede,
} from "./skjema";

export function InnsendingKort({
	nummer,
	innsending,
	resultat,
	onEndring,
	onFjern,
}: {
	nummer: number;
	innsending: InnsendingSkjema;
	resultat: InnsendingResultat;
	onEndring: (innsending: InnsendingSkjema) => void;
	/** Utelates når innsendingen ikke kan fjernes (det må alltid være minst én) */
	onFjern?: () => void;
}) {
	const sett = (endring: Partial<InnsendingSkjema>) =>
		onEndring({ ...innsending, ...endring });
	const settKjeder = (kjeder: KjedeSkjema[]) => sett({ kjeder });
	const headingId = `innsending-${innsending.nokkel}`;

	// En kjede kan bare fortsettes én gang per innsending
	const ledigeKjeder = resultat.kjedeforslag.filter(
		(forslag) =>
			!innsending.kjeder.some((k) => k.fortsetterKjedeId === forslag.id),
	);

	return (
		<Box
			as="section"
			aria-labelledby={headingId}
			borderWidth="1"
			borderColor="neutral"
			borderRadius="12"
			padding="space-24"
		>
			<VStack gap="space-16">
				<HStack justify="space-between" align="center">
					<Heading id={headingId} level="2" size="medium">
						Innsending {nummer}
					</Heading>
					{onFjern && (
						<Button
							size="small"
							variant="tertiary"
							icon={<TrashIcon aria-hidden />}
							onClick={onFjern}
						>
							Fjern innsending
						</Button>
					)}
				</HStack>

				<HGrid columns={{ xs: 1, lg: 2 }} gap="space-24" align="start">
					<VStack gap="space-16">
						<HGrid columns={{ xs: 1, sm: 2 }} gap="space-12">
							<TextField
								label="kodeFagomraade"
								size="small"
								value={innsending.felter.kodeFagomraade ?? ""}
								onChange={(e) =>
									sett({
										felter: {
											...innsending.felter,
											kodeFagomraade: e.target.value,
										},
									})
								}
							/>
							<TextField
								label="fagsystemId"
								size="small"
								readOnly
								value={resultat.fagsystemId ?? ""}
							/>
						</HGrid>

						<Accordion size="small">
							<Accordion.Item>
								<Accordion.Header>oppdrag-110</Accordion.Header>
								<Accordion.Content>
									<FeltGruppe
										definisjoner={oppdragsfelter}
										verdier={innsending.felter}
										onEndring={(felter) => sett({ felter })}
									/>
								</Accordion.Content>
							</Accordion.Item>
							<Accordion.Item>
								<Accordion.Header>avstemming-115</Accordion.Header>
								<Accordion.Content>
									<FeltGruppe
										definisjoner={avstemmingsfelter}
										verdier={innsending.felter}
										onEndring={(felter) => sett({ felter })}
									/>
								</Accordion.Content>
							</Accordion.Item>
							<Accordion.Item>
								<Accordion.Header>ompostering-116</Accordion.Header>
								<Accordion.Content>
									<FeltGruppe
										definisjoner={omposteringsfelter}
										verdier={innsending.ompostering}
										onEndring={(ompostering) => sett({ ompostering })}
									/>
								</Accordion.Content>
							</Accordion.Item>
							<Accordion.Item>
								<Accordion.Header>
									avvent-118 ({innsending.avventer.length})
								</Accordion.Header>
								<Accordion.Content>
									<ListeFelter
										tittel="Avvent"
										elementnavn="avvent"
										definisjoner={avventfelter}
										elementer={innsending.avventer}
										onEndring={(avventer) => sett({ avventer })}
									/>
								</Accordion.Content>
							</Accordion.Item>
							<Accordion.Item>
								<Accordion.Header>
									oppdrags-enhet-120 ({innsending.enheter.length})
								</Accordion.Header>
								<Accordion.Content>
									<ListeFelter
										tittel="Enhet"
										elementnavn="enhet"
										definisjoner={enhetsfelter}
										elementer={innsending.enheter}
										onEndring={(enheter) => sett({ enheter })}
									/>
								</Accordion.Content>
							</Accordion.Item>
							<Accordion.Item>
								<Accordion.Header>
									tekst-140 ({innsending.tekster.length})
								</Accordion.Header>
								<Accordion.Content>
									<ListeFelter
										tittel="Tekst"
										elementnavn="tekst"
										definisjoner={tekstfelter}
										elementer={innsending.tekster}
										onEndring={(tekster) => sett({ tekster })}
									/>
								</Accordion.Content>
							</Accordion.Item>
						</Accordion>

						{innsending.kjeder.map((kjede, index) => (
							<KjedeKort
								key={kjede.nokkel}
								kjede={kjede}
								kjedeId={resultat.kjedeIder[index]}
								onEndring={(ny) =>
									settKjeder(
										innsending.kjeder.map((k) => (k === kjede ? ny : k)),
									)
								}
								onFjern={() =>
									settKjeder(innsending.kjeder.filter((k) => k !== kjede))
								}
							/>
						))}

						<div>
							<ActionMenu>
								<ActionMenu.Trigger>
									<Button
										size="small"
										variant="secondary"
										icon={<ChevronDownIcon aria-hidden />}
										iconPosition="right"
									>
										Ny kjede
									</Button>
								</ActionMenu.Trigger>
								<ActionMenu.Content>
									<ActionMenu.Item
										onSelect={() =>
											settKjeder([...innsending.kjeder, nyKjede()])
										}
									>
										Ny kjede
									</ActionMenu.Item>
									{ledigeKjeder.length > 0 && (
										<>
											<ActionMenu.Divider />
											<ActionMenu.Group label="Eksisterende kjeder">
												{ledigeKjeder.map((forslag) => (
													<ActionMenu.Item
														key={forslag.id}
														onSelect={() =>
															settKjeder([
																...innsending.kjeder,
																nyKjede(forslag.id),
															])
														}
													>
														Kjede {forslag.id}
													</ActionMenu.Item>
												))}
											</ActionMenu.Group>
										</>
									)}
								</ActionMenu.Content>
							</ActionMenu>
						</div>
					</VStack>

					<VStack gap="space-8" className={styles.forhandsvisning}>
						<Heading level="3" size="xsmall">
							XML
						</Heading>
						{resultat.feil ? (
							<InlineMessage status="error">{resultat.feil}</InlineMessage>
						) : (
							<Box
								background="neutral-soft"
								borderRadius="8"
								padding="space-12"
								overflowX="auto"
							>
								<pre className={styles.xml}>
									{formaterXml(resultat.xml ?? "")}
								</pre>
							</Box>
						)}
					</VStack>
				</HGrid>
			</VStack>
		</Box>
	);
}
