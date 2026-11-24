import { init } from "@nais/apm";

function fjernUrlFragmenter<T extends object>(item: T): T {
	const besokt = new WeakSet<object>();
	const rens = (verdi: object) => {
		if (besokt.has(verdi)) return;
		besokt.add(verdi);
		for (const [felt, innhold] of Object.entries(verdi)) {
			if (
				typeof innhold === "string" &&
				(innhold.startsWith("https://") || innhold.startsWith("http://"))
			) {
				Object.assign(verdi, { [felt]: innhold.split("#")[0] });
			} else if (typeof innhold === "object" && innhold !== null) {
				rens(innhold);
			}
		}
	};
	rens(item);
	return item;
}

export function initApm() {
	init({
		namespace: "beregningsplattform",
		app: "beregningsplattformen-lab-frontend",
		tracing: true,
		devConsoleEcho: false,
		beforeSend: fjernUrlFragmenter,
	});
}
