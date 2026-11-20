export type TemplateFieldKind = "pnr" | "dato" | "number";

export type TemplateField = {
	key: string;
	name: string;
	kind: TemplateFieldKind;
	value: string;
	oppdragslinje?: number;
};

const typedPlaceholder =
	/<([\w-]+)>([^<]*?)\{(pnr|dato|number)\}([^<]*?)<\/\1>/g;
const oppdragslinje150 =
	/<oppdrags-linje-150\b[^>]*>[\s\S]*?<\/oppdrags-linje-150\s*>/g;

export function templateFields(template: string): TemplateField[] {
	const fields: TemplateField[] = [];
	const occurrences = new Map<string, number>();
	const oppdragslinjer = Array.from(
		template.matchAll(oppdragslinje150),
		(match) => ({
			start: match.index,
			end: match.index + match[0].length,
		}),
	);

	for (const match of template.matchAll(typedPlaceholder)) {
		const [, name, value, kind] = match;
		const markerIndex = match.index;
		const type = kind as TemplateFieldKind;
		const occurrenceKey = `${name}:${type}`;
		const occurrence = occurrences.get(occurrenceKey) ?? 0;
		occurrences.set(occurrenceKey, occurrence + 1);
		const oppdragslinjeIndex = oppdragslinjer.findIndex(
			(range) => markerIndex >= range.start && markerIndex < range.end,
		);
		fields.push({
			key: `${occurrenceKey}:${occurrence}`,
			name,
			kind: type,
			value,
			...(oppdragslinjeIndex >= 0
				? { oppdragslinje: oppdragslinjeIndex + 1 }
				: {}),
		});
	}

	return fields;
}
