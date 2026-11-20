// Bare for visning: builderen lager XML på én linje, så vi lager linjeskift og innrykk ut fra dybden
export function formaterXml(xml: string): string {
	let dybde = 0;
	return xml
		.replace(/>\s+</g, "><")
		.trim()
		.replace(/></g, ">\n<")
		.split("\n")
		.map((linje) => {
			if (linje.startsWith("</")) dybde--;
			const innrykket = `${"    ".repeat(Math.max(dybde, 0))}${linje}`;
			if (
				!linje.startsWith("</") &&
				!linje.includes("</") &&
				!linje.endsWith("/>")
			)
				dybde++;
			return innrykket;
		})
		.join("\n");
}
