import {
	BodyShort,
	Box,
	Detail,
	Heading,
	HGrid,
	Table,
	VStack,
} from "@navikt/ds-react";
import { DataGrid } from "@navikt/ds-react/PREVIEW/DataGrid";
import type { BeregningResponse } from "../types/BeregningResponse";
import styles from "./Beregningsresultater.module.css";

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

function totalsum(delberegninger: Beregning["delberegninger"]) {
	const beløp = delberegninger.map((del) => del.beloep);
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
			columns={{ xs: 1, sm: 2, lg: 3 }}
			gap="space-8"
			margin="space-0"
		>
			<Felt label="ID" verdi={String(beregning.id)} />
			<Felt label="Gjelder" verdi={beregning.gjelder} />
			<Felt label="Status" verdi={beregning.status} />
			<Felt label="KID" verdi={beregning.kidnummer ?? "Ikke oppgitt"} />
			<Felt label="Mottaker" verdi={beregning.utbetalesTil} />
			<Felt label="Totalsum" verdi={totalsum(beregning.delberegninger)} />
		</HGrid>
	);
}

function Linjedetaljer({ delberegning }: { delberegning: Delberegning }) {
	if (delberegning.linjer.length === 0) {
		return (
			<BodyShort>Ingen linjer i delberegning {delberegning.id}.</BodyShort>
		);
	}

	return (
		<Table
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
					<Table.Row key={linje.id}>
						<Table.HeaderCell scope="row">{linje.id}</Table.HeaderCell>
						<Table.DataCell>{linje.type}</Table.DataCell>
						<Table.DataCell>{linje.sats ?? "Ikke oppgitt"}</Table.DataCell>
						<Table.DataCell align="right">{linje.beloep}</Table.DataCell>
					</Table.Row>
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

function Beregningsgruppe({ beregning }: { beregning: Beregning }) {
	const kolonner: DataGrid.Columns<Delberegning> = [
		{
			id: "id",
			header: "Delberegning",
			isRowHeader: true,
			bodyCell: (delberegning) => delberegning.id,
		},
		{
			id: "periode",
			header: "Periode",
			bodyCell: (delberegning) =>
				`${formaterDato(delberegning.fom)} – ${formaterDato(delberegning.tom)}`,
		},
		{
			id: "fagomraade",
			header: "Fagområde",
			bodyCell: (delberegning) => delberegning.fagomraade,
		},
		{
			id: "beloep",
			header: "Beløp",
			align: "right",
			bodyCell: (delberegning) => delberegning.beloep,
		},
	];
	const detaljpanel: DataGrid.Table.DetailsPanel<Delberegning> = {
		getContent: (delberegning) => (
			<VStack
				as="section"
				gap="space-12"
				minWidth="0"
				padding="space-16"
				aria-label={`Linjer for delberegning ${delberegning.id}`}
			>
				<Linjedetaljer delberegning={delberegning} />
			</VStack>
		),
	};

	return (
		<Box
			as="article"
			minWidth="0"
			padding="space-20"
			borderColor="neutral"
			borderWidth="1"
			borderRadius="12"
			background="meta-purple-soft"
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
				{beregning.delberegninger.length === 0 ? (
					<BodyShort>Beregningen inneholder ingen delberegninger.</BodyShort>
				) : (
					<Box minWidth="0" overflowX="auto">
						<DataGrid
							columns={kolonner}
							data={beregning.delberegninger}
							getRowId={(delberegning) => String(delberegning.id)}
							defaultSettings={{ columnDividers: false }}
						>
							<DataGrid.Table
								layout="auto"
								stickyHeader={false}
								emptyContent={<BodyShort>Ingen delberegninger.</BodyShort>}
								detailsPanel={detaljpanel}
							/>
						</DataGrid>
					</Box>
				)}
			</VStack>
		</Box>
	);
}

export function Beregningsresultater({
	beregninger,
}: {
	beregninger: Beregninger;
}) {
	return (
		<Box
			as="section"
			width="100%"
			maxWidth="90rem"
			padding="space-32"
			borderColor="neutral-subtle"
			borderWidth="1"
			borderRadius="12"
			background="meta-purple-moderateA"
			aria-labelledby="beregningsresultater-heading"
		>
			<VStack gap="space-16">
				<Heading id="beregningsresultater-heading" level="2" size="medium">
					Beregningsresultater
				</Heading>
				{beregninger.length === 0 ? (
					<BodyShort>Responsen inneholder ingen beregninger.</BodyShort>
				) : (
					<VStack gap="space-16">
						{beregninger.map((beregning) => (
							<Beregningsgruppe key={beregning.id} beregning={beregning} />
						))}
					</VStack>
				)}
			</VStack>
		</Box>
	);
}
