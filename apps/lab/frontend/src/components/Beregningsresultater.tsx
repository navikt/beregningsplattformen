import {
	BodyShort,
	Box,
	Detail,
	Heading,
	HGrid,
	HStack,
	Switch,
	Table,
	VStack,
} from "@navikt/ds-react";
import { Fragment, useState } from "react";
import type { BeregningResponse } from "../types/BeregningResponse";
import styles from "./Beregningsresultater.module.css";
import { EkstraInfoDetaljer } from "./EkstraInfoDetaljer";

type Beregninger = NonNullable<
	BeregningResponse["beregningstidslinje"]
>["beregninger"];
type Beregning = Beregninger[number];
type Delberegning = Beregning["delberegninger"][number];

function formaterDato(dato: string) {
	const [år, måned, dag] = dato.split("-");
	return `${dag}.${måned}.${år}`;
}

function Felt({ label, verdi }: { label: string; verdi: string }) {
	return (
		<Box
			className={styles.beregningsresultater__felt}
			background="default"
			borderRadius="8"
			minWidth="0"
			padding="space-8"
			paddingInline="space-12"
		>
			<dt>
				<Detail as="span" textColor="subtle" weight="semibold">
					{label}
				</Detail>
			</dt>
			<Box as="dd" margin="space-0">
				<BodyShort as="span">{verdi}</BodyShort>
			</Box>
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
		<HGrid
			as="dl"
			columns={{ xs: 1, sm: 2, lg: 4 }}
			gap="space-8"
			margin="space-0"
		>
			<Felt label="Gjelder" verdi={beregning.gjelder} />
			<Felt label="Status" verdi={beregning.status} />
			<Felt label="KID" verdi={beregning.kidnummer ?? "Ikke oppgitt"} />
			<Felt label="Mottaker" verdi={beregning.utbetalesTil} />
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
		<HGrid as="dl" columns={{ xs: 1, sm: 3 }} gap="space-8" margin="space-0">
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
		<Table
			className={styles.beregningsresultater__linjer}
			size="small"
			aria-label={`Linjer for delberegning ${delberegning.id}`}
		>
			<Table.Header>
				<Table.Row>
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
					<Fragment key={linje.id}>
						<Table.Row>
							<Table.HeaderCell scope="row">{linje.id}</Table.HeaderCell>
							<Table.DataCell>{linje.type}</Table.DataCell>
							<Table.DataCell>{linje.sats ?? "Ikke oppgitt"}</Table.DataCell>
							<Table.DataCell align="right">{linje.beloep}</Table.DataCell>
						</Table.Row>
						{visEkstraInfo && (linje.ekstraInfo?.length ?? 0) > 0 && (
							<Table.Row>
								<Table.DataCell colSpan={4}>
									<EkstraInfoDetaljer ekstraInfo={linje.ekstraInfo} />
								</Table.DataCell>
							</Table.Row>
						)}
					</Fragment>
				))}
			</Table.Body>
			<tfoot>
				<Table.Row>
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

function Beregningsgruppe({
	beregning,
	visEkstraInfo,
}: {
	beregning: Beregning;
	visEkstraInfo: boolean;
}) {
	return (
		<Box
			as="article"
			minWidth="0"
			padding="space-20"
			borderColor="neutral"
			borderWidth="1"
			borderRadius="12"
			background="info-soft"
			aria-labelledby={`beregning-heading-${beregning.id}`}
		>
			<VStack gap="space-12">
				<Heading
					id={`beregning-heading-${beregning.id}`}
					level="3"
					size="small"
				>
					Beregning {beregning.id}
				</Heading>
				<Metadata beregning={beregning} />
				{visEkstraInfo && (
					<EkstraInfoDetaljer
						ekstraInfo={beregning.ekstraInfo}
						tittel="Ekstrainfo for beregningen"
					/>
				)}
				{beregning.delberegninger.length === 0 ? (
					<BodyShort>Beregningen inneholder ingen delberegninger.</BodyShort>
				) : (
					<Box minWidth="0" overflowX="auto">
						<Table
							className={styles.beregningsresultater__delberegninger}
							zebraStripes
							aria-labelledby={`beregning-heading-${beregning.id}`}
						>
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
										colSpan={5}
										contentGutter="none"
										content={
											<VStack
												as="section"
												gap="space-12"
												minWidth="0"
												padding="space-16"
												aria-label={`Detaljer for delberegning ${delberegning.id}`}
											>
												{visEkstraInfo && (
													<EkstraInfoDetaljer
														ekstraInfo={delberegning.ekstraInfo}
														tittel="Ekstrainfo for delberegningen"
													/>
												)}
												<Linjedetaljer
													delberegning={delberegning}
													visEkstraInfo={visEkstraInfo}
												/>
											</VStack>
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
					</Box>
				)}
				<Summeringer beregning={beregning} />
			</VStack>
		</Box>
	);
}

export function Beregningsresultater({
	beregninger,
}: {
	beregninger: Beregninger;
}) {
	const [visEkstraInfo, settVisEkstraInfo] = useState(false);

	return (
		<Box
			as="section"
			width="100%"
			maxWidth="90rem"
			padding="space-32"
			borderColor="neutral-subtle"
			borderWidth="1"
			borderRadius="12"
			background="info-moderateA"
			aria-labelledby="beregningsresultater-heading"
		>
			<VStack gap="space-16">
				<HStack align="center" justify="space-between" gap="space-16" wrap>
					<Heading id="beregningsresultater-heading" level="2" size="medium">
						Beregningsresultater
					</Heading>
					{beregninger.length > 0 && (
						<Switch
							checked={visEkstraInfo}
							onChange={(event) => settVisEkstraInfo(event.target.checked)}
						>
							Vis ekstrainfo
						</Switch>
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
							/>
						))}
					</VStack>
				)}
			</VStack>
		</Box>
	);
}
