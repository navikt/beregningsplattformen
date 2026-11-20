import { ChevronDownIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	Accordion,
	ActionMenu,
	Box,
	Button,
	CopyButton,
	Heading,
	HGrid,
	HStack,
	InlineMessage,
	TextField,
	VStack,
} from "@navikt/ds-react";
import { useRef, useState } from "react";
import {
	avstemmingsfelter,
	avventfelter,
	enhetsfelter,
	omposteringsfelter,
	oppdragsfelter,
	tekstfelter,
} from "./feltdefinisjoner";
import { formaterXml } from "./formaterXml";
import { KjedeKort } from "./KjedeKort";
import { FeltGruppe, ListeFelter } from "./Skjemafelter";
import {
	type InnsendingResultat,
	type InnsendingSkjema,
	type KjedeSkjema,
	nyKjede,
} from "./skjema";
import { XmlVisning } from "./XmlVisning";

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
	const xmlHeadingId = `${headingId}-xml`;
	const nyKjedeRef = useRef<HTMLButtonElement>(null);
	const [nyKjedeNokkel, setNyKjedeNokkel] = useState<string>();
	const leggTilKjede = (kjede: KjedeSkjema) => {
		setNyKjedeNokkel(kjede.nokkel);
		settKjeder([...innsending.kjeder, kjede]);
	};

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
			background="default"
		>
			<VStack gap="space-16">
				<HStack justify="space-between" align="center">
					<Heading id={headingId} level="3" size="medium">
						Innsending {nummer}
					</Heading>
					{onFjern && (
						<Button
							size="small"
							variant="tertiary"
							icon={<TrashIcon aria-hidden />}
							onClick={onFjern}
						>
							Fjern innsending {nummer}
						</Button>
					)}
				</HStack>

				<HGrid columns={{ xs: 1, lg: 2 }} gap="space-24" align="start">
					<VStack gap="space-16">
						<HGrid columns={{ xs: 1, sm: 2 }} gap="space-12">
							<TextField
								label="kodeFagomraade"
								description="Må fylles ut"
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
								description="Genereres automatisk"
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
										legend="oppdrag-110"
										hideLegend
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
										legend="avstemming-115"
										hideLegend
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
										legend="ompostering-116"
										hideLegend
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
								fokuser={kjede.nokkel === nyKjedeNokkel}
								onEndring={(ny) =>
									settKjeder(
										innsending.kjeder.map((k) => (k === kjede ? ny : k)),
									)
								}
								onFjern={() => {
									settKjeder(innsending.kjeder.filter((k) => k !== kjede));
									nyKjedeRef.current?.focus();
								}}
							/>
						))}

						<HStack>
							<ActionMenu>
								<ActionMenu.Trigger>
									<Button
										ref={nyKjedeRef}
										size="small"
										variant="secondary"
										icon={<ChevronDownIcon aria-hidden />}
										iconPosition="right"
									>
										Ny kjede
									</Button>
								</ActionMenu.Trigger>
								<ActionMenu.Content>
									<ActionMenu.Item onSelect={() => leggTilKjede(nyKjede())}>
										Ny kjede
									</ActionMenu.Item>
									{ledigeKjeder.length > 0 && (
										<>
											<ActionMenu.Divider />
											<ActionMenu.Group label="Eksisterende kjeder">
												{ledigeKjeder.map((forslag) => (
													<ActionMenu.Item
														key={forslag.id}
														onSelect={() => leggTilKjede(nyKjede(forslag.id))}
													>
														Kjede {forslag.id}
													</ActionMenu.Item>
												))}
											</ActionMenu.Group>
										</>
									)}
								</ActionMenu.Content>
							</ActionMenu>
						</HStack>
					</VStack>

					<VStack gap="space-8" position="sticky" top="space-16" minWidth="0">
						<HStack justify="space-between" align="center">
							<Heading id={xmlHeadingId} level="4" size="xsmall">
								XML
							</Heading>
							{!resultat.feil && (
								<CopyButton
									size="small"
									copyText={formaterXml(resultat.xml ?? "")}
									text="Kopier XML"
									activeText="Kopiert"
								/>
							)}
						</HStack>
						{/* Live-region som alltid finnes, så skjermlesere får med seg feil som dukker opp mens man skriver */}
						<Box aria-live="polite">
							{resultat.feil && (
								<InlineMessage status="error">{resultat.feil}</InlineMessage>
							)}
						</Box>
						{!resultat.feil && (
							<Box
								as="section"
								aria-labelledby={xmlHeadingId}
								// Kan scrolles sidelengs, så tastaturbrukere må kunne nå den (WCAG 2.1.1)
								tabIndex={0}
								background="neutral-soft"
								borderRadius="8"
								padding="space-12"
								overflowX="auto"
							>
								<Box asChild margin="space-0">
									{/* Aksel har ingen fontstørrelse under --ax-font-size-small (14px), som er større enn standard for pre */}
									<pre style={{ fontSize: "0.8125rem" }}>
										<XmlVisning xml={formaterXml(resultat.xml ?? "")} />
									</pre>
								</Box>
							</Box>
						)}
					</VStack>
				</HGrid>
			</VStack>
		</Box>
	);
}
