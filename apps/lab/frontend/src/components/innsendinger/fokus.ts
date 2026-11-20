import { useEffect, useRef } from "react";

const skjemafelt = "input, select, textarea";

/**
 * Flytter fokus til et nytt element når det vises, så tastatur- og skjermleserbrukere havner der de la til noe.
 * Er ref-elementet selv ikke et skjemafelt, får det første skjemafeltet inni fokus.
 */
export function useFokusVedVisning<T extends HTMLElement>(aktiv: boolean) {
	const ref = useRef<T>(null);
	// biome-ignore lint/correctness/useExhaustiveDependencies: skal bare kjøre når elementet vises første gang
	useEffect(() => {
		if (!aktiv) return;
		// Venter til ActionMenu har gitt fokus tilbake til knappen som åpnet den, ellers overstyres vi
		const tidsavbrudd = setTimeout(() => {
			const element = ref.current;
			const maal = element?.matches(`${skjemafelt}, button`)
				? element
				: element?.querySelector<HTMLElement>(skjemafelt);
			maal?.focus();
		});
		return () => clearTimeout(tidsavbrudd);
	}, []);
	return ref;
}
