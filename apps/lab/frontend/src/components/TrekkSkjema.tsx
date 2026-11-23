import {PlusIcon, TrashIcon} from "@navikt/aksel-icons";
import {
    BodyShort,
    Box,
    Button,
    ErrorSummary,
    Fieldset,
    Heading,
    HGrid,
    HStack,
    Radio,
    RadioGroup,
    Select,
    TextField,
    VStack,
} from "@navikt/ds-react";
import {useEffect, useRef, useState} from "react";
import {
    nyPeriode,
    nyttTrekk,
    type TrekkFeil,
    type TrekkPeriodeUtkast,
    type TrekkUtkast,
    trekkPeriodeFeltId,
    trekkTrekkFeltId,
} from "../types/Trekk";
import {OppdragDatoFelt} from "./OppdragDatoFelt";

interface Props {
    utkast: TrekkUtkast[];
    feil: TrekkFeil[];
    sender: boolean;
    /** Tar imot en oppdateringsfunksjon, siden datofeltene kan melde flere endringer samtidig. */
    onChange: (oppdater: (utkast: TrekkUtkast[]) => TrekkUtkast[]) => void;
}

export function TrekkSkjema({utkast, feil, sender, onChange}: Props) {
    const oppsummering = useRef<HTMLDivElement>(null);
    const [fokusId, settFokusId] = useState<string | null>(null);
    useEffect(() => {
        if (feil.length > 0) oppsummering.current?.focus();
    }, [feil]);
    useEffect(() => {
        if (fokusId) {
            document.getElementById(fokusId)?.focus();
            settFokusId(null);
        }
    }, [fokusId]);

    const feilmelding = (felt: string) =>
        feil.find((enFeil) => enFeil.felt === felt)?.melding;
    // Feilmeldingen står ved feltet, så prefikset med trekk og periode er bare nødvendig i oppsummeringen.
    const feltfeil = (felt: string) =>
        feilmelding(felt)?.replace(/^Trekk \d+(?:, periode \d+)?: /, "");

    const endreTrekk = (id: string, endring: Partial<TrekkUtkast>) =>
        onChange((trekkene) =>
            trekkene.map((trekk) =>
                trekk.id === id ? {...trekk, ...endring} : trekk,
            ),
        );
    const endrePeriode = (
        trekkId: string,
        periodeId: string,
        endring: Partial<TrekkPeriodeUtkast>,
    ) =>
        onChange((trekkene) =>
            trekkene.map((trekk) =>
                trekk.id === trekkId
                    ? {
                        ...trekk,
                        trekkPerioder: trekk.trekkPerioder.map((periode) =>
                            periode.id === periodeId ? {...periode, ...endring} : periode,
                        ),
                    }
                    : trekk,
            ),
        );

    return (
        <Box
            as="section"
            padding="space-24"
            background="default"
            borderWidth="1"
            borderColor="neutral-subtle"
            borderRadius="12"
            aria-labelledby="trekk-heading"
        >
            <VStack gap="space-16">
                <Heading id="trekk-heading" level="2" size="medium">
                    Syntetiske trekkoppgaver
                </Heading>
                <BodyShort>
                    Bruk bare syntetiske data. Trekkene gjelder hele innsendingen. Uten
                    trekk beregnes ingen trekk.
                </BodyShort>
                {feil.length > 0 && (
                    <ErrorSummary
                        ref={oppsummering}
                        heading="Rett feilene i trekkoppgavene"
                        headingTag="h3"
                    >
                        {feil.map(({felt, melding}) => (
                            <ErrorSummary.Item key={`${felt}-${melding}`} href={`#${felt}`}>
                                {melding}
                            </ErrorSummary.Item>
                        ))}
                    </ErrorSummary>
                )}
                <Fieldset legend="Trekkoppgaver" hideLegend disabled={sender}>
                    <VStack gap="space-24">
                        {utkast.map((trekk, trekkIndeks) => {
                            const felt = (navn: string) => trekkTrekkFeltId(trekk.id, navn);
                            const nummer = trekkIndeks + 1;
                            return (
                                <Box
                                    key={trekk.id}
                                    padding="space-16"
                                    borderWidth="1"
                                    borderColor="neutral-subtle"
                                    borderRadius="8"
                                >
                                    <Fieldset legend={`Trekk ${nummer}`}>
                                        <VStack gap="space-16">
                                            <HGrid
                                                columns={{xs: 1, md: 2}}
                                                gap="space-16"
                                                align="start"
                                            >
                                                <TextField
                                                    id={felt("trekkType")}
                                                    label="Trekktypekode"
                                                    value={trekk.trekkType}
                                                    error={feltfeil(felt("trekkType"))}
                                                    onChange={(event) =>
                                                        endreTrekk(trekk.id, {
                                                            trekkType: event.target.value,
                                                        })
                                                    }
                                                />
                                                <Select
                                                    id={felt("beregningsType")}
                                                    label="Beregningstype"
                                                    value={trekk.beregningsType}
                                                    error={feltfeil(felt("beregningsType"))}
                                                    onChange={(event) =>
                                                        endreTrekk(trekk.id, {
                                                            beregningsType: event.target.value,
                                                        })
                                                    }
                                                >
                                                    <option value="YTELPER">YTELPER</option>
                                                    <option value="UTBDATO">UTBDATO</option>
                                                </Select>
                                                <RadioGroup
                                                    id={felt("skatteReduserende")}
                                                    legend="Reduserer skatt"
                                                    value={trekk.skatteReduserende}
                                                    error={feltfeil(felt("skatteReduserende"))}
                                                    onChange={(verdi: boolean) =>
                                                        endreTrekk(trekk.id, {skatteReduserende: verdi})
                                                    }
                                                >
                                                    <HStack gap={"space-12"}>
                                                        <Radio value={true}>Ja</Radio>
                                                        <Radio value={false}>Nei</Radio>
                                                    </HStack>
                                                </RadioGroup>
                                                <TextField
                                                    id={felt("saldo")}
                                                    label="Saldo (kroner, valgfri)"
                                                    description="La stå tomt for ingen begrensning. 0 betyr at saldoen er brukt opp."
                                                    inputMode="decimal"
                                                    value={trekk.saldo}
                                                    error={feltfeil(felt("saldo"))}
                                                    onChange={(event) =>
                                                        endreTrekk(trekk.id, {saldo: event.target.value})
                                                    }
                                                />
                                                <OppdragDatoFelt
                                                    id={felt("gyldigTom")}
                                                    label="Gyldig til (valgfri)"
                                                    value={trekk.gyldigTom}
                                                    readOnly={sender}
                                                    error={feltfeil(felt("gyldigTom"))}
                                                    onChange={(gyldigTom) =>
                                                        endreTrekk(trekk.id, {gyldigTom})
                                                    }
                                                    onValidityChange={(gyldigTomGyldig) =>
                                                        endreTrekk(trekk.id, {gyldigTomGyldig})
                                                    }
                                                />
                                            </HGrid>
                                            <Fieldset
                                                legend={`Perioder for trekk ${nummer}`}
                                                error={feltfeil(felt("perioder"))}
                                            >
                                                <VStack gap="space-16">
                                                    {trekk.trekkPerioder.map((periode, periodeIndeks) => {
                                                        const periodeFelt = (navn: string) =>
                                                            trekkPeriodeFeltId(trekk.id, periode.id, navn);
                                                        return (
                                                            <HStack
                                                                key={periode.id}
                                                                role="group"
                                                                aria-label={`Trekk ${nummer}, periode ${periodeIndeks + 1}`}
                                                                gap="space-16"
                                                                align="start"
                                                                wrap
                                                            >
                                                                <TextField
                                                                    id={periodeFelt("sats")}
                                                                    label={`Sats, periode ${periodeIndeks + 1}`}
                                                                    inputMode="decimal"
                                                                    htmlSize={10}
                                                                    value={periode.sats}
                                                                    error={feltfeil(periodeFelt("sats"))}
                                                                    onChange={(event) =>
                                                                        endrePeriode(trekk.id, periode.id, {
                                                                            sats: event.target.value,
                                                                        })
                                                                    }
                                                                />
                                                                <Select
                                                                    id={periodeFelt("satsType")}
                                                                    label={`Satstype, periode ${periodeIndeks + 1}`}
                                                                    value={periode.satsType}
                                                                    error={feltfeil(periodeFelt("satsType"))}
                                                                    onChange={(event) =>
                                                                        endrePeriode(trekk.id, periode.id, {
                                                                            satsType: event.target.value,
                                                                        })
                                                                    }
                                                                >
                                                                    <option value="D">Dagsats (D)</option>
                                                                    <option value="M">Månedssats (M)</option>
                                                                </Select>
                                                                <OppdragDatoFelt
                                                                    id={periodeFelt("fom")}
                                                                    label={`Fra og med, periode ${periodeIndeks + 1}`}
                                                                    value={periode.fom}
                                                                    readOnly={sender}
                                                                    error={feltfeil(periodeFelt("fom"))}
                                                                    onChange={(fom) =>
                                                                        endrePeriode(trekk.id, periode.id, {fom})
                                                                    }
                                                                    onValidityChange={(fomGyldig) =>
                                                                        endrePeriode(trekk.id, periode.id, {
                                                                            fomGyldig,
                                                                        })
                                                                    }
                                                                />
                                                                <OppdragDatoFelt
                                                                    id={periodeFelt("tom")}
                                                                    label={`Til og med, periode ${periodeIndeks + 1}`}
                                                                    value={periode.tom}
                                                                    readOnly={sender}
                                                                    error={feltfeil(periodeFelt("tom"))}
                                                                    onChange={(tom) =>
                                                                        endrePeriode(trekk.id, periode.id, {tom})
                                                                    }
                                                                    onValidityChange={(tomGyldig) =>
                                                                        endrePeriode(trekk.id, periode.id, {
                                                                            tomGyldig,
                                                                        })
                                                                    }
                                                                />
                                                                {trekk.trekkPerioder.length > 1 && (
                                                                    <Button
                                                                        type="button"
                                                                        variant="tertiary"
                                                                        size="small"
                                                                        icon={<TrashIcon aria-hidden/>}
                                                                        disabled={sender}
                                                                        onClick={() => {
                                                                            endreTrekk(trekk.id, {
                                                                                trekkPerioder:
                                                                                    trekk.trekkPerioder.filter(
                                                                                        (denne) => denne.id !== periode.id,
                                                                                    ),
                                                                            });
                                                                            settFokusId(felt("leggTilPeriode"));
                                                                        }}
                                                                    >
                                                                        {`Fjern periode ${periodeIndeks + 1} fra trekk ${nummer}`}
                                                                    </Button>
                                                                )}
                                                            </HStack>
                                                        );
                                                    })}
                                                    <HStack>
                                                        <Button
                                                            id={felt("leggTilPeriode")}
                                                            type="button"
                                                            variant="secondary"
                                                            size="small"
                                                            icon={<PlusIcon aria-hidden/>}
                                                            disabled={sender}
                                                            onClick={() => {
                                                                const ny = nyPeriode();
                                                                endreTrekk(trekk.id, {
                                                                    trekkPerioder: [...trekk.trekkPerioder, ny],
                                                                });
                                                                settFokusId(
                                                                    trekkPeriodeFeltId(trekk.id, ny.id, "sats"),
                                                                );
                                                            }}
                                                        >
                                                            {`Legg til periode i trekk ${nummer}`}
                                                        </Button>
                                                    </HStack>
                                                </VStack>
                                            </Fieldset>
                                            <HStack>
                                                <Button
                                                    type="button"
                                                    variant="tertiary"
                                                    size="small"
                                                    icon={<TrashIcon aria-hidden/>}
                                                    disabled={sender}
                                                    onClick={() => {
                                                        onChange((trekkene) =>
                                                            trekkene.filter((denne) => denne.id !== trekk.id),
                                                        );
                                                        settFokusId("trekk-legg-til");
                                                    }}
                                                >
                                                    {`Fjern trekk ${nummer}`}
                                                </Button>
                                            </HStack>
                                        </VStack>
                                    </Fieldset>
                                </Box>
                            );
                        })}
                        <HStack>
                            <Button
                                id="trekk-legg-til"
                                type="button"
                                variant="secondary"
                                icon={<PlusIcon aria-hidden/>}
                                disabled={sender}
                                onClick={() => {
                                    const nytt = nyttTrekk();
                                    onChange((trekkene) => [...trekkene, nytt]);
                                    settFokusId(trekkTrekkFeltId(nytt.id, "trekkType"));
                                }}
                            >
                                Legg til trekk
                            </Button>
                        </HStack>
                    </VStack>
                </Fieldset>
            </VStack>
        </Box>
    );
}
