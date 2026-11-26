import {
	BodyShort,
	Box,
	Fieldset,
	Heading,
	HGrid,
	Select,
	Switch,
	TextField,
	VStack,
} from "@navikt/ds-react";
import {
	type ForskuddstrekkUtkast,
	type SkattekortUtkast,
	faggrupperForTrekkode,
	tabellnumre,
	trekkoder,
} from "../types/Skattekort";

interface Props {
	utkast: SkattekortUtkast;
	sender: boolean;
	onChange: (utkast: SkattekortUtkast) => void;
}

export function SkattekortSkjema({ utkast, sender, onChange }: Props) {
	const trekk = utkast.forskuddstrekk;
	const felt = (navn: string) => `forskuddstrekk-0-${navn}`;
	const endreTrekk = (endring: Partial<ForskuddstrekkUtkast>) =>
		onChange({
			...utkast,
			forskuddstrekk: { ...trekk, ...endring },
		});

	return (
		<Box
			as="section"
			padding="space-24"
			background="raised"
			borderWidth="1"
			borderColor="neutral-subtle"
			borderRadius="12"
			aria-labelledby="skattekort-heading"
		>
			<VStack gap="space-16">
				<Heading id="skattekort-heading" level="2" size="medium">
					Skattekort
				</Heading>
				<Switch
					size="small"
					checked={utkast.aktiv}
					disabled={sender}
					onChange={(event) =>
						onChange({ ...utkast, aktiv: event.target.checked })
					}
				>
					Bruk syntetisk skattekort
				</Switch>
				{utkast.aktiv && (
					<>
						<Fieldset
							legend="Skattekortopplysninger"
							hideLegend
							disabled={sender}
							size="small"
						>
							<VStack gap="space-24">
								<TextField
									id="skattekort-inntektsaar"
									label="Inntektsår"
									inputMode="numeric"
									htmlSize={6}
									value={utkast.inntektsaar}
									onChange={(event) =>
										onChange({ ...utkast, inntektsaar: event.target.value })
									}
								/>
								<Fieldset legend="Forskuddstrekk">
									<HGrid columns={{ xs: 1, md: 2 }} gap="space-16" align="end">
										<Select
											id={`skattekort-${felt("trekkode")}`}
											label="Trekkode"
											value={trekk.trekkode}
											onChange={(event) =>
												endreTrekk({
													trekkode: event.target.value,
													faggruppe:
														faggrupperForTrekkode(event.target.value)[0] ?? "",
												})
											}
										>
											{Object.entries(trekkoder).map(([kode, navn]) => (
												<option key={kode} value={kode}>
													{navn}
												</option>
											))}
										</Select>
										<Select
											id={`skattekort-${felt("type")}`}
											label="Korttype"
											value={trekk.type}
											onChange={(event) =>
												endreTrekk({ type: event.target.value })
											}
										>
											<option value="PROSENTKORT">Prosentkort</option>
											<option value="TABELLKORT">Tabellkort</option>
											<option value="FRIKORT">Frikort</option>
										</Select>
										{trekk.type !== "FRIKORT" && (
											<TextField
												id={`skattekort-${felt("prosentsats")}`}
												label="Prosentsats"
												description={
													trekk.type === "TABELLKORT"
														? "Brukes ved etterbetaling og for faggrupper uten tabellallokering."
														: "Prosent mellom 0 og 100, med høyst to desimaler."
												}
												inputMode="decimal"
												value={trekk.prosentsats}
												onChange={(event) =>
													endreTrekk({ prosentsats: event.target.value })
												}
											/>
										)}
										{trekk.type === "TABELLKORT" && (
											<>
												<Select
													id={`skattekort-${felt("tabellnummer")}`}
													label="Tabellnummer"
													value={trekk.tabellnummer}
													onChange={(event) =>
														endreTrekk({ tabellnummer: event.target.value })
													}
												>
													{tabellnumre.map((nummer) => (
														<option key={nummer} value={nummer}>
															{nummer}
														</option>
													))}
												</Select>
												<Select
													id={`skattekort-${felt("faggruppe")}`}
													label="Faggruppe for tabelltrekk"
													description="Andre faggrupper bruker prosentsatsen."
													value={trekk.faggruppe}
													onChange={(event) =>
														endreTrekk({ faggruppe: event.target.value })
													}
												>
													<option value="">Velg faggruppe</option>
													{faggrupperForTrekkode(trekk.trekkode).map(
														(gruppe) => (
															<option key={gruppe} value={gruppe}>
																{gruppe}
															</option>
														),
													)}
												</Select>
											</>
										)}
										{trekk.type === "FRIKORT" && (
											<TextField
												id={`skattekort-${felt("frikortbeloep")}`}
												label="Frikortbeløp (kroner)"
												description="Tilgjengelig frikortsaldo ved beregningsstart. Oppgi hele kroner. 0 betyr at frikortet er brukt opp."
												inputMode="numeric"
												value={trekk.frikortbeloep}
												onChange={(event) =>
													endreTrekk({ frikortbeloep: event.target.value })
												}
											/>
										)}
									</HGrid>
								</Fieldset>
							</VStack>
						</Fieldset>
					</>
				)}
			</VStack>
		</Box>
	);
}
