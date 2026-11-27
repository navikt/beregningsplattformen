import {
	BodyShort,
	Box,
	Button,
	Detail,
	Heading,
	HGrid,
	HStack,
	InlineMessage,
	Switch,
	Table,
	VStack,
} from "@navikt/ds-react";
import { type ReactNode, useState } from "react";
import type {
	BeregningResponse,
	EkstraInfo,
} from "../../../types/BeregningResponse";
import { ChevronDownIcon, ChevronUpIcon } from "@navikt/aksel-icons";

type Beregninger = NonNullable<
	BeregningResponse["beregningstidslinje"]
>["beregninger"];
type Beregning = Beregninger[number];
type Delberegning = Beregning["delberegninger"][number];

function formaterDato(dato: string) {
	const [år, måned, dag] = dato.split("-");
	return `${dag}.${måned}.${år}`;
}

function formaterDatoTid(datoTid: string) {
	const [dato, tid] = datoTid.split("T");
	return `${formaterDato(dato)} ${tid.split(".")[0]}`;
}

function formaterEkstraInfo(info: EkstraInfo): string {
	if (!info.template) return info.tekst ?? info.grunn;

	return info.template.replace(
		/\{([^{}:]+)(?::[^{}]+)?\}/g,
		(plassholder: string, navn: string) => {
			const verdi = info.verdier[navn];
			if (!verdi) return plassholder;
			return verdi.skjult ? "***SKJULT***" : verdi.verdi;
		},
	);
}

function Felt({ label, verdi }: { label: string; verdi: string }) {
	return (
		<Box
			background="neutral-softA"
			borderRadius="8"
			borderColor="neutral-subtle"
			borderWidth="1"
			padding="space-8"
			paddingInline="space-12"
			asChild
		>
			<VStack>
				<Detail textColor="subtle" weight="semibold">
					{label}
				</Detail>
				<BodyShort>{verdi}</BodyShort>
			</VStack>
		</Box>
	);
}

function totalsum(poster: { beloep: string }[]) {
	const beløp = poster.map((post) => post.beloep);
	if (beløp.some((verdi) => !/^-?\d+(?:\.\d+)?$/.test(verdi))) {
		return "Kan ikke summeres";
	}
	const desimaler = beløp.reduce(
		(maks, verdi) => Math.max(maks, verdi.split(".")[1]?.length ?? 0),
		0,
	);
	// Summer med samme desimalskala som beløpene fra backend, uten flyttallsavrunding.
	const sum = beløp.reduce((total, verdi) => {
		const [heltall, brøk = ""] = verdi.split(".");
		return total + BigInt(heltall + brøk.padEnd(desimaler, "0"));
	}, 0n);
	if (desimaler === 0) return sum.toString();
	const sifre = (sum < 0n ? -sum : sum).toString().padStart(desimaler + 1, "0");
	return `${sum < 0n ? "-" : ""}${sifre.slice(0, -desimaler)},${sifre.slice(-desimaler)}`;
}

function Metadata({ beregning }: { beregning: Beregning }) {
	return (
		<HGrid columns={{ xs: 1, sm: 2, lg: 5 }} gap="space-8">
			<Felt label="Gjelder" verdi={beregning.gjelder} />
			<Felt label="Status" verdi={beregning.status} />
			<Felt label="KID" verdi={beregning.kidnummer ?? "Ikke oppgitt"} />
			<Felt label="Mottaker" verdi={beregning.utbetalesTil} />
			{beregning.beregnet && (
				<Felt label="Beregnet" verdi={formaterDatoTid(beregning.beregnet)} />
			)}
		</HGrid>
	);
}

function Summeringer({ beregning }: { beregning: Beregning }) {
	const ytelseslinjer = beregning.delberegninger.flatMap((del) =>
		del.linjer.filter((linje) => linje.type === "YTELSE"),
	);
	const skattelinjer = beregning.delberegninger.flatMap((del) =>
		del.linjer.filter((linje) => linje.type === "SKATT"),
	);

	return (
		<HGrid columns={{ xs: 1, sm: 3 }} gap="space-8">
			<Felt label="Brutto" verdi={totalsum(ytelseslinjer)} />
			<Felt label="Samlet skattetrekk" verdi={totalsum(skattelinjer)} />
			<Felt label="Netto" verdi={totalsum(beregning.delberegninger)} />
		</HGrid>
	);
}

function Linjedetaljer({
	delberegning,
	visEkstraInfo,
}: {
	delberegning: Delberegning;
	visEkstraInfo: boolean;
}) {
	if (delberegning.linjer.length === 0) {
		return (
			<BodyShort>Ingen linjer i delberegning {delberegning.id}.</BodyShort>
		);
	}

	return (
		<Table size="small">
			<Table.Header>
				<Table.Row style={{ background: "transparent" }}>
					<Table.HeaderCell scope="col">Linje-ID</Table.HeaderCell>
					<Table.HeaderCell scope="col">Type</Table.HeaderCell>
					<Table.HeaderCell scope="col">Sats</Table.HeaderCell>
					<Table.HeaderCell scope="col" align="right">
						Beløp
					</Table.HeaderCell>
				</Table.Row>
			</Table.Header>
			<Table.Body>
				{delberegning.linjer.map((linje) => (
					<>
						<Table.Row>
							<Table.HeaderCell scope="row">{linje.id}</Table.HeaderCell>
							<Table.DataCell>{linje.type}</Table.DataCell>
							<Table.DataCell align="right">
								<HStack gap="space-4">{linje.sats ?? "Ikke oppgitt"}</HStack>
							</Table.DataCell>
							<Table.DataCell align="right">{linje.beloep}</Table.DataCell>
						</Table.Row>
						{visEkstraInfo &&
							linje.ekstraInfo &&
							linje.ekstraInfo?.length > 0 && (
								<Table.Row>
									<Table.DataCell colSpan={4}>
										<InlineMessage status="info">
											{linje.ekstraInfo?.map((info) =>
												formaterEkstraInfo(info),
											)}
										</InlineMessage>
									</Table.DataCell>
								</Table.Row>
							)}
					</>
				))}
			</Table.Body>
			<tfoot>
				<Table.Row
					style={{ background: "var(--ax-bg-neutral-moderate-hover)" }}
				>
					<Table.HeaderCell scope="row" colSpan={3}>
						Sum
					</Table.HeaderCell>
					<Table.DataCell align="right">
						<strong>{delberegning.beloep}</strong>
					</Table.DataCell>
				</Table.Row>
			</tfoot>
		</Table>
	);
}

function radNøkkel(beregning: Beregning, delberegning: Delberegning) {
	return `${beregning.id}:${delberegning.id}`;
}

function Beregningsgruppe({
	beregning,
	visEkstraInfo,
	åpneRader,
	settRadÅpen,
}: {
	beregning: Beregning;
	visEkstraInfo: boolean;
	åpneRader: Set<string>;
	settRadÅpen: (nøkkel: string, åpen: boolean) => void;
}) {
	return (
		<Box
			padding="space-20"
			borderColor="neutral"
			borderWidth="2"
			borderRadius="12"
			background="default"
			asChild
		>
			<VStack gap="space-12">
				<Heading level="3" size="small">
					Beregning {beregning.id}
				</Heading>
				<Metadata beregning={beregning} />
				{beregning.delberegninger.length === 0 ? (
					<BodyShort>Beregningen inneholder ingen delberegninger.</BodyShort>
				) : (
					<Table zebraStripes>
						<Table.Header>
							<Table.Row>
								<Table.HeaderCell scope="col" />
								<Table.HeaderCell scope="col">Delberegning</Table.HeaderCell>
								<Table.HeaderCell scope="col">Periode</Table.HeaderCell>
								<Table.HeaderCell scope="col">Fagområde</Table.HeaderCell>
								<Table.HeaderCell scope="col" align="right">
									Beløp
								</Table.HeaderCell>
							</Table.Row>
						</Table.Header>
						<Table.Body>
							{beregning.delberegninger.map((delberegning) => (
								<Table.ExpandableRow
									key={delberegning.id}
									colSpan={6}
									open={åpneRader.has(radNøkkel(beregning, delberegning))}
									onOpenChange={(åpen) =>
										settRadÅpen(radNøkkel(beregning, delberegning), åpen)
									}
									content={
										<HStack justify="end">
											<Linjedetaljer
												delberegning={delberegning}
												visEkstraInfo={visEkstraInfo}
											/>
										</HStack>
									}
								>
									<Table.HeaderCell scope="row">
										{delberegning.id}
									</Table.HeaderCell>
									<Table.DataCell>
										{formaterDato(delberegning.fom)} –{" "}
										{formaterDato(delberegning.tom)}
									</Table.DataCell>
									<Table.DataCell>{delberegning.fagomraade}</Table.DataCell>
									<Table.DataCell align="right">
										{delberegning.beloep}
									</Table.DataCell>
								</Table.ExpandableRow>
							))}
						</Table.Body>
					</Table>
				)}
				<Summeringer beregning={beregning} />
			</VStack>
		</Box>
	);
}

export function Beregningsresultater({
	beregninger,
	kontroller,
}: {
	beregninger: Beregninger;
	kontroller?: ReactNode;
}) {
	const [visEkstraInfo, settVisEkstraInfo] = useState(false);
	const [åpneRader, settÅpneRader] = useState<Set<string>>(new Set());

	const alleRader = beregninger.flatMap((beregning) =>
		beregning.delberegninger.map((del) => radNøkkel(beregning, del)),
	);
	const alleErÅpne =
		alleRader.length > 0 && alleRader.every((nøkkel) => åpneRader.has(nøkkel));

	function settRadÅpen(nøkkel: string, åpen: boolean) {
		settÅpneRader((forrige) => {
			const neste = new Set(forrige);
			if (åpen) neste.add(nøkkel);
			else neste.delete(nøkkel);
			return neste;
		});
	}

	return (
		<Box
			width="100%"
			padding="space-32"
			borderColor="neutral-subtle"
			borderWidth="1"
			borderRadius="12"
			background="info-moderateA"
		>
			<VStack gap="space-16">
				<HStack align="center" justify="space-between" gap="space-16">
					<HStack align="center" gap="space-16">
						<Heading level="2" size="medium">
							Beregningsresultater
						</Heading>
						{beregninger.length > 0 && kontroller && (
							<Box style={{ flexShrink: 0, whiteSpace: "nowrap" }}>
								{kontroller}
							</Box>
						)}
					</HStack>
					{beregninger.length > 0 && (
						<HStack align="center" gap="space-16">
							{alleRader.length > 0 && (
								<Button
									type="button"
									variant="tertiary"
									data-color="neutral"
									size="small"
									onClick={() =>
										settÅpneRader(alleErÅpne ? new Set() : new Set(alleRader))
									}
									icon={alleErÅpne ? <ChevronUpIcon /> : <ChevronDownIcon />}
									iconPosition="right"
								>
									{alleErÅpne ? "Lukk alle rader" : "Ekspander alle rader"}
								</Button>
							)}
							<Switch
								checked={visEkstraInfo}
								onChange={(event) => settVisEkstraInfo(event.target.checked)}
							>
								Vis ekstrainfo
							</Switch>
						</HStack>
					)}
				</HStack>
				{beregninger.length === 0 ? (
					<BodyShort>Ingen beregninger å vise.</BodyShort>
				) : (
					<VStack gap="space-16">
						{beregninger.map((beregning) => (
							<Beregningsgruppe
								key={beregning.id}
								beregning={beregning}
								visEkstraInfo={visEkstraInfo}
								åpneRader={åpneRader}
								settRadÅpen={settRadÅpen}
							/>
						))}
					</VStack>
				)}
			</VStack>
		</Box>
	);
}
