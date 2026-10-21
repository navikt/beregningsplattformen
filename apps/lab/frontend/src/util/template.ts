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

export function fillTypedTemplate(
	template: string,
	fields: TemplateField[],
	values: Record<string, string>,
): string {
	const defaults = new Map(fields.map((field) => [field.key, field.value]));
	const occurrences = new Map<string, number>();

	return template.replace(
		typedPlaceholder,
		(
			_match,
			name: string,
			_prefix: string,
			kind: TemplateFieldKind,
			suffix: string,
		) => {
			const occurrenceKey = `${name}:${kind}`;
			const occurrence = occurrences.get(occurrenceKey) ?? 0;
			occurrences.set(occurrenceKey, occurrence + 1);
			const key = `${occurrenceKey}:${occurrence}`;
			const value = values[key] ?? defaults.get(key) ?? "";
			const generatedValue = kind === "dato" ? formatXmlDate(value) : value;

			return `<${name}>${escapeXml(generatedValue)}${escapeXml(suffix)}</${name}>`;
		},
	);
}

function escapeXml(value: string): string {
	return value
		.replaceAll("&", "&amp;")
		.replaceAll("<", "&lt;")
		.replaceAll(">", "&gt;");
}

function formatXmlDate(date: string): string {
	if (!date) return "";

	if (/^\d{4}-\d{2}-\d{2}(?:T.*)?(?:Z|[+-]\d{2}:?\d{2})$/i.test(date)) {
		return date;
	}
	if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return date;

	const dateAtNoonUtc = new Date(`${date}T12:00:00Z`);
	if (Number.isNaN(dateAtNoonUtc.getTime())) return date;

	const offset = new Intl.DateTimeFormat("en", {
		timeZone: "Europe/Oslo",
		timeZoneName: "longOffset",
	})
		.formatToParts(dateAtNoonUtc)
		.find((part) => part.type === "timeZoneName")?.value;

	return offset ? `${date}${offset.replace("GMT", "")}` : date;
}
