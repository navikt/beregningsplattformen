import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
	Box,
	Button,
	DatePicker,
	Fieldset,
	HGrid,
	HStack,
	TextField,
	UNSAFE_Combobox,
	useDatepicker,
	VStack,
} from "@navikt/ds-react";
import { useRef, useState } from "react";
import type { FeltDefinisjon, Forslag } from "./feltdefinisjoner";
import { useFokusVedVisning } from "./fokus";
import { type MedNokkel, nyNokkel } from "./skjema";

type Verdier = Partial<Record<string, string>>;

// Lokal tid, ikke toISOString(), som kan gi dagen før
function tilIsoDato(dato: Date): string {
	const to = (tall: number) => String(tall).padStart(2, "0");
	return `${dato.getFullYear()}-${to(dato.getMonth() + 1)}-${to(dato.getDate())}`;
}

function fraIsoDato(verdi?: string): Date | undefined {
	const treff = verdi?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
	if (!treff) return undefined;
	return new Date(Number(treff[1]), Number(treff[2]) - 1, Number(treff[3]));
}

function DatoFelt({
	label,
	verdi,
	onEndring,
}: {
	label: string;
	verdi?: string;
	onEndring: (verdi?: string) => void;
}) {
	// useDatepicker sender undefined når teksten ikke er en gyldig dato, så vi tar vare på teksten selv
	const skrevetTekst = useRef("");
	// En lagret egen verdi kan ikke gis som defaultSelected, så den vises til brukeren skriver noe
	const [startTekst, setStartTekst] = useState(
		fraIsoDato(verdi) ? "" : (verdi ?? ""),
	);
	const { datepickerProps, inputProps } = useDatepicker({
		defaultSelected: fraIsoDato(verdi),
		onDateChange: (dato) => {
			if (dato) skrevetTekst.current = "";
			onEndring(
				dato ? tilIsoDato(dato) : skrevetTekst.current.trim() || undefined,
			);
		},
	});
	return (
		<DatePicker {...datepickerProps}>
			<DatePicker.Input
				{...inputProps}
				value={inputProps.value || startTekst}
				onChange={(e) => {
					skrevetTekst.current = e.target.value;
					setStartTekst("");
					inputProps.onChange?.(e);
				}}
				label={label}
				size="small"
			/>
		</DatePicker>
	);
}

// Som DatoFelt, men lagrer midnatt i lokal tid. Bare dato ville blitt tolket som UTC av builderen.
const midnatt = "T00:00:00";

function TidspunktFelt({
	label,
	verdi,
	onEndring,
}: {
	label: string;
	verdi?: string;
	onEndring: (verdi?: string) => void;
}) {
	return (
		<DatoFelt
			label={label}
			verdi={verdi?.endsWith(midnatt) ? verdi.slice(0, -midnatt.length) : verdi}
			onEndring={(dato) =>
				onEndring(fraIsoDato(dato) ? `${dato}${midnatt}` : dato)
			}
		/>
	);
}

// En egen verdi finnes ikke i forslagene, så den vises som seg selv
function valgtForslag(forslag: Forslag[], verdi?: string): Forslag[] {
	if (!verdi) return [];
	return [
		forslag.find((f) => f.value === verdi) ?? { label: verdi, value: verdi },
	];
}

export function FeltGruppe<T extends Verdier>({
	legend,
	hideLegend,
	definisjoner,
	verdier,
	onEndring,
}: {
	legend: string;
	/** Skjul legend når en synlig overskrift rett over allerede navngir gruppen */
	hideLegend?: boolean;
	definisjoner: FeltDefinisjon<T>[];
	verdier: T;
	onEndring: (verdier: T) => void;
}) {
	const sett = (navn: keyof T, verdi?: string) =>
		onEndring({ ...verdier, [navn]: verdi });

	return (
		<Fieldset legend={legend} hideLegend={hideLegend} size="small">
			{/* paddingBlock gir plass til fokusrammen, Accordion.Content klipper alt utenfor */}
			<HGrid
				columns={{ xs: 1, sm: 2, md: 3 }}
				gap="space-12"
				paddingBlock="space-8"
			>
				{definisjoner.map((felt) =>
					felt.tidspunkt ? (
						<TidspunktFelt
							key={felt.navn}
							label={felt.label}
							verdi={verdier[felt.navn]}
							onEndring={(verdi) => sett(felt.navn, verdi)}
						/>
					) : felt.dato ? (
						<DatoFelt
							key={felt.navn}
							label={felt.label}
							verdi={verdier[felt.navn]}
							onEndring={(verdi) => sett(felt.navn, verdi)}
						/>
					) : felt.forslag ? (
						<UNSAFE_Combobox
							key={felt.navn}
							label={felt.label}
							description={felt.beskrivelse}
							size="small"
							options={felt.forslag}
							allowNewValues
							selectedOptions={valgtForslag(felt.forslag, verdier[felt.navn])}
							onToggleSelected={(verdi, valgt) =>
								sett(felt.navn, valgt ? verdi : undefined)
							}
						/>
					) : (
						<TextField
							key={felt.navn}
							label={felt.label}
							description={felt.beskrivelse}
							size="small"
							value={verdier[felt.navn] ?? ""}
							onChange={(e) => sett(felt.navn, e.target.value)}
						/>
					),
				)}
			</HGrid>
		</Fieldset>
	);
}

function ListeElement<T extends Verdier>({
	legend,
	fjernTekst,
	definisjoner,
	element,
	fokuser,
	onEndring,
	onFjern,
}: {
	legend: string;
	fjernTekst: string;
	definisjoner: FeltDefinisjon<T>[];
	element: MedNokkel<T>;
	fokuser: boolean;
	onEndring: (element: MedNokkel<T>) => void;
	onFjern: () => void;
}) {
	const ref = useFokusVedVisning<HTMLDivElement>(fokuser);
	return (
		<Box
			ref={ref}
			borderWidth="1"
			borderColor="neutral"
			borderRadius="8"
			padding="space-12"
		>
			<VStack gap="space-8">
				<FeltGruppe
					legend={legend}
					definisjoner={definisjoner}
					verdier={element}
					onEndring={(ny) => onEndring({ ...element, ...ny })}
				/>
				<HStack justify="end">
					<Button
						size="xsmall"
						variant="tertiary"
						icon={<TrashIcon aria-hidden />}
						onClick={onFjern}
					>
						{fjernTekst}
					</Button>
				</HStack>
			</VStack>
		</Box>
	);
}

export function ListeFelter<T extends Verdier>({
	tittel,
	elementnavn,
	definisjoner,
	elementer,
	onEndring,
}: {
	tittel: string;
	elementnavn: string;
	definisjoner: FeltDefinisjon<T>[];
	elementer: MedNokkel<T>[];
	onEndring: (elementer: MedNokkel<T>[]) => void;
}) {
	const leggTilRef = useRef<HTMLButtonElement>(null);
	const [nyNokkelVerdi, setNyNokkelVerdi] = useState<string>();

	return (
		<VStack gap="space-12" paddingBlock="space-8">
			{elementer.map((element, index) => (
				<ListeElement
					key={element.nokkel}
					legend={`${tittel} ${index + 1}`}
					fjernTekst={`Fjern ${elementnavn} ${index + 1}`}
					definisjoner={definisjoner}
					element={element}
					fokuser={element.nokkel === nyNokkelVerdi}
					onEndring={(ny) =>
						onEndring(elementer.map((e) => (e === element ? ny : e)))
					}
					onFjern={() => {
						onEndring(elementer.filter((e) => e !== element));
						leggTilRef.current?.focus();
					}}
				/>
			))}
			<HStack>
				<Button
					ref={leggTilRef}
					size="small"
					variant="secondary"
					icon={<PlusIcon aria-hidden />}
					onClick={() => {
						const nytt = { nokkel: nyNokkel() } as MedNokkel<T>;
						setNyNokkelVerdi(nytt.nokkel);
						onEndring([...elementer, nytt]);
					}}
				>
					Legg til {elementnavn}
				</Button>
			</HStack>
		</VStack>
	);
}
