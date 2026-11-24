import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { useEffect } from "react";

/* TODO: Bjørn mener denne finnes bare fordi aksel7 hadde issue med tømming av datovelger kanskje hele denne komponenten kan fjernes */

interface OppdragDatoFeltProps {
	id?: string;
	label: string;
	hideLabel?: boolean;
	size?: "medium" | "small";
	strategy?: "absolute" | "fixed";
	value: string;
	readOnly: boolean;
	onChange: (value: string) => void;
	error?: string;
	onValidityChange?: (gyldig: boolean) => void;
}

function parseDato(value: string): Date | undefined {
	const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
	if (!match) return undefined;

	const year = Number(match[1]);
	const month = Number(match[2]);
	const day = Number(match[3]);
	const date = new Date(0);
	date.setFullYear(year, month - 1, day);
	date.setHours(0, 0, 0, 0);

	if (
		date.getFullYear() !== year ||
		date.getMonth() !== month - 1 ||
		date.getDate() !== day
	) {
		return undefined;
	}

	return date;
}

function formatDato(date: Date): string {
	const year = String(date.getFullYear()).padStart(4, "0");
	const month = String(date.getMonth() + 1).padStart(2, "0");
	const day = String(date.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

function sammeDato(left?: Date, right?: Date): boolean {
	return (
		left?.getFullYear() === right?.getFullYear() &&
		left?.getMonth() === right?.getMonth() &&
		left?.getDate() === right?.getDate()
	);
}

export function OppdragDatoFelt({
	id,
	label,
	hideLabel,
	size,
	strategy,
	value,
	readOnly,
	onChange,
	error,
	onValidityChange,
}: OppdragDatoFeltProps) {
	const { datepickerProps, inputProps, selectedDay, setSelected } =
		useDatepicker({
			defaultSelected: parseDato(value),
			inputFormat: "dd.MM.yyyy",
			onDateChange: (date) => onChange(date ? formatDato(date) : ""),
			onValidate: ({ isValidDate, isEmpty }) =>
				onValidityChange?.(isValidDate || isEmpty),
		});

	useEffect(() => {
		const selected = parseDato(value);
		if (!sammeDato(selected, selectedDay)) {
			setSelected(selected);
		}
	}, [selectedDay, setSelected, value]);

	return (
		<DatePicker {...datepickerProps} strategy={strategy}>
			<DatePicker.Input
				{...inputProps}
				id={id}
				label={label}
				hideLabel={hideLabel}
				size={size}
				readOnly={readOnly}
				error={error}
			/>
		</DatePicker>
	);
}
