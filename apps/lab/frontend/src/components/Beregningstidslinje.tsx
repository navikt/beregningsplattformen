import {
	BankNoteFillIcon,
	ChevronLeftIcon,
	ChevronRightIcon,
	MinusCircleFillIcon,
	PercentIcon,
} from "@navikt/aksel-icons";
import {
	BodyShort,
	Box,
	Button,
	CopyButton,
	Heading,
	Timeline,
	ToggleGroup,
	VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import styles from "../pages/SendInnOppdragsXML.module.css";
import type {
	BeregningDTO,
	BeregningstidslinjeDTO,
	DelberegningDTO,
	LinjeDTO,
	LinjetypeDTO,
} from "../types/BeregningstidslinjeDTO";

interface Linjepølse {
	id: string;
	beregning: BeregningDTO;
	delberegning: DelberegningDTO;
	linje: LinjeDTO;
}

const linjetypeRekkefølge: Record<LinjetypeDTO, number> = {
	YTELSE: 0,
	SKATT: 1,
	TREKK: 2,
};

const visualiseringForLinjetype: Record<
	LinjetypeDTO,
	{
		status: "success" | "warning" | "danger";
		icon: React.ReactNode;
		statusLabel: string;
	}
> = {
	YTELSE: {
		status: "success",
		icon: <BankNoteFillIcon aria-hidden />,
		statusLabel: "Ytelse",
	},
	SKATT: {
		status: "warning",
		icon: <PercentIcon aria-hidden />,
		statusLabel: "Skatt",
	},
	TREKK: {
		status: "danger",
		icon: <MinusCircleFillIcon aria-hidden />,
		statusLabel: "Trekk",
	},
};

function formaterNorskDato(dato: string) {
	const [år, måned, dag] = dato.split("-");
	return `${dag}.${måned}.${år}`;
}

function formaterPeriode(delberegning: DelberegningDTO) {
	return `${formaterNorskDato(delberegning.fom)} - ${formaterNorskDato(delberegning.tom)}`;
}

function formaterBeløp(beløp: string) {
	const tall = Number(beløp);
	return Number.isNaN(tall)
		? beløp
		: `${tall.toLocaleString("nb-NO", { maximumFractionDigits: 2 })} kr`;
}

// Fordeler linjene på rader slik at overlappende linjer havner under hverandre.
function fordelPåRader(beregning: BeregningDTO): Linjepølse[][] {
	const pølser = beregning.delberegninger
		.flatMap((delberegning) =>
			delberegning.linjer.map((linje) => ({
				id: `beregning-${beregning.id}-${delberegning.id}-${linje.id}`,
				beregning,
				delberegning,
				linje,
			})),
		)
		.sort(
			(a, b) =>
				a.delberegning.fom.localeCompare(b.delberegning.fom) ||
				linjetypeRekkefølge[a.linje.type] - linjetypeRekkefølge[b.linje.type],
		);

	const rader: Linjepølse[][] = [];
	for (const pølse of pølser) {
		const ledigRad = rader.find(
			(rad) => rad[rad.length - 1].delberegning.tom < pølse.delberegning.fom,
		);
		if (ledigRad) {
			ledigRad.push(pølse);
		} else {
			rader.push([pølse]);
		}
	}
	return rader;
}

function Detaljgruppe({
	tittel,
	children,
}: {
	tittel: string;
	children: React.ReactNode;
}) {
	return (
		<div>
			<Heading level="3" size="xsmall">
				{tittel}
			</Heading>
			<dl className={styles["laboratorium__periodedetaljer-liste"]}>
				{children}
			</dl>
		</div>
	);
}

function Detalj({ label, verdi }: { label: string; verdi: string }) {
	return (
		<div>
			<dt>{label}</dt>
			<dd>
				<span className={styles.laboratorium__detaljverdi}>
					<span className={styles.laboratorium__detaljverditekst}>{verdi}</span>
					<CopyButton
						size="xsmall"
						copyText={verdi}
						aria-label={`Kopier ${label}`}
					/>
				</span>
			</dd>
		</div>
	);
}

function førsteDagIMåneden(dato: Date) {
	return new Date(dato.getFullYear(), dato.getMonth(), 1);
}

function tidligsteFom(beregninger: BeregningDTO[]) {
	const fomDatoer = beregninger.flatMap((beregning) =>
		beregning.delberegninger.map((delberegning) => delberegning.fom),
	);
	if (fomDatoer.length === 0) return førsteDagIMåneden(new Date());
	return førsteDagIMåneden(new Date(fomDatoer.sort()[0]));
}

export function Beregningstidslinje({
	beregningstidslinje,
}: {
	beregningstidslinje: BeregningstidslinjeDTO;
}) {
	const { beregninger } = beregningstidslinje;
	const [valgtPølse, settValgtPølse] = useState<Linjepølse | null>(null);
	const [antallMåneder, settAntallMåneder] = useState(4);
	const [startDato, settStartDato] = useState(() => tidligsteFom(beregninger));
	const sluttDato = new Date(
		startDato.getFullYear(),
		startDato.getMonth() + antallMåneder,
		0,
	);
	const flyttTidslinje = (måneder: number) =>
		settStartDato(
			(start) => new Date(start.getFullYear(), start.getMonth() + måneder, 1),
		);

	if (beregninger.length === 0) {
		return <BodyShort>Ingen beregninger å vise.</BodyShort>;
	}

	return (
		<>
			<div className={styles.laboratorium__tidslinjenavigasjon}>
				<div className={styles.laboratorium__tidslinjekontroller}>
					<ToggleGroup
						aria-label="Antall måneder i beregningsperioden"
						data-color="neutral"
						size="small"
						value={String(antallMåneder)}
						onChange={(value) => settAntallMåneder(Number(value))}
					>
						<ToggleGroup.Item value="2" label="2 måneder" />
						<ToggleGroup.Item value="4" label="4 måneder" />
						<ToggleGroup.Item value="8" label="8 måneder" />
					</ToggleGroup>
					<div className={styles.laboratorium__tidslinjepaginering}>
						<Button
							type="button"
							data-color="neutral"
							variant="secondary"
							size="small"
							icon={<ChevronLeftIcon aria-hidden />}
							aria-label="Forrige beregningsperiode"
							onClick={() => flyttTidslinje(-antallMåneder)}
						/>
						<Button
							type="button"
							data-color="neutral"
							variant="secondary"
							size="small"
							icon={<ChevronRightIcon aria-hidden />}
							aria-label="Neste beregningsperiode"
							onClick={() => flyttTidslinje(antallMåneder)}
						/>
					</div>
				</div>
			</div>
			<section
				className={styles.laboratorium__kjede}
				aria-label="Beregningstidslinje"
			>
				<Timeline startDate={startDato} endDate={sluttDato}>
					{beregninger.flatMap((beregning) =>
						fordelPåRader(beregning).map((rad, radindeks) => {
							const radId = `rad-beregning-${beregning.id}-${radindeks}`;
							return (
								<Timeline.Row
									key={radId}
									id={radId}
									label={
										radindeks === 0 ? (
											<div className={styles.laboratorium__tidslinjemetadata}>
												<BodyShort as="h3" size="small" weight="semibold">
													Beregning {beregning.id}
												</BodyShort>
												<span>{beregning.status}</span>
											</div>
										) : (
											<span className="aksel-sr-only">
												Beregning {beregning.id}, rad {radindeks + 1}
											</span>
										)
									}
								>
									{rad.map((pølse) => {
										const { delberegning, linje } = pølse;
										const visualisering = visualiseringForLinjetype[linje.type];
										const periodeTekst = formaterPeriode(delberegning);
										return (
											<Timeline.Period
												key={pølse.id}
												id={pølse.id}
												aria-controls="beregningsdetaljer"
												isActive={valgtPølse?.id === pølse.id}
												onSelectPeriod={() =>
													settValgtPølse((forrige) =>
														forrige?.id === pølse.id ? null : pølse,
													)
												}
												start={new Date(delberegning.fom)}
												end={new Date(delberegning.tom)}
												status={visualisering.status}
												icon={visualisering.icon}
												statusLabel={visualisering.statusLabel}
												title={`${visualisering.statusLabel} ${periodeTekst}`}
											>
												<p>
													{visualisering.statusLabel}:{" "}
													{formaterBeløp(linje.beloep)}
												</p>
												<p>{periodeTekst}</p>
											</Timeline.Period>
										);
									})}
								</Timeline.Row>
							);
						}),
					)}
				</Timeline>
			</section>
			<Box id="beregningsdetaljer" aria-live="polite">
				{valgtPølse && (
					<section
						className={styles.laboratorium__periodedetaljer}
						aria-label="Detaljer for valgt linje"
					>
						<VStack gap="space-24">
							<Detaljgruppe tittel={`Linje #${valgtPølse.linje.id}`}>
								<Detalj
									label="Type"
									verdi={
										visualiseringForLinjetype[valgtPølse.linje.type].statusLabel
									}
								/>
								<Detalj label="Sats" verdi={valgtPølse.linje.sats ?? "Ingen"} />
								<Detalj
									label="Beløp"
									verdi={formaterBeløp(valgtPølse.linje.beloep)}
								/>
							</Detaljgruppe>
							<Detaljgruppe
								tittel={`Delberegning #${valgtPølse.delberegning.id}`}
							>
								<Detalj
									label="Periode"
									verdi={formaterPeriode(valgtPølse.delberegning)}
								/>
								<Detalj
									label="Fagområde"
									verdi={valgtPølse.delberegning.fagomraade}
								/>
								<Detalj
									label="Beløp"
									verdi={formaterBeløp(valgtPølse.delberegning.beloep)}
								/>
							</Detaljgruppe>
							<Detaljgruppe tittel={`Beregning #${valgtPølse.beregning.id}`}>
								<Detalj label="Status" verdi={valgtPølse.beregning.status} />
								<Detalj label="Gjelder" verdi={valgtPølse.beregning.gjelder} />
								<Detalj
									label="Utbetales til"
									verdi={valgtPølse.beregning.utbetalesTil}
								/>
								<Detalj
									label="Kidnummer"
									verdi={valgtPølse.beregning.kidnummer ?? "Ingen"}
								/>
							</Detaljgruppe>
						</VStack>
					</section>
				)}
			</Box>
		</>
	);
}
