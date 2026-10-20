import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { useEffect } from "react";

interface OppdragDatoFeltProps {
	label: string;
	value: string;
	readOnly: boolean;
	onChange: (value: string) => void;
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
	label,
	value,
	readOnly,
	onChange,
}: OppdragDatoFeltProps) {
	const { datepickerProps, inputProps, selectedDay, setSelected } =
		useDatepicker({
			defaultSelected: parseDato(value),
			inputFormat: "dd.MM.yyyy",
			onDateChange: (date) => onChange(date ? formatDato(date) : ""),
		});

	useEffect(() => {
		const selected = parseDato(value);
		if (!sammeDato(selected, selectedDay)) {
			setSelected(selected);
		}
	}, [selectedDay, setSelected, value]);

	return (
		<DatePicker {...datepickerProps}>
			<DatePicker.Input {...inputProps} label={label} readOnly={readOnly} />
		</DatePicker>
	);
}
