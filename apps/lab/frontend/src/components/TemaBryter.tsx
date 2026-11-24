import {
	CheckmarkIcon,
	MonitorIcon,
	MoonIcon,
	SunIcon,
	ThemeIcon,
} from "@navikt/aksel-icons";
import { ActionMenu, Button, HStack, Tooltip } from "@navikt/ds-react";
import {
	type ReactNode,
	useEffect,
	useState,
	useSyncExternalStore,
} from "react";

export type Temavalg = "light" | "dark" | "system";
export type Tema = "light" | "dark";

const mork = "(prefers-color-scheme: dark)";
const lagringsnokkel = "temavalg";

function lesLagretValg(): Temavalg {
	try {
		const lagret = localStorage.getItem(lagringsnokkel);
		if (lagret === "light" || lagret === "dark" || lagret === "system") {
			return lagret;
		}
	} catch {
		// localStorage kan være utilgjengelig
	}
	return "system";
}

function abonnerPaSystemtema(onEndring: () => void) {
	const sporring = window.matchMedia(mork);
	sporring.addEventListener("change", onEndring);
	return () => sporring.removeEventListener("change", onEndring);
}

export function useTema() {
	const [valg, settValgState] = useState<Temavalg>(lesLagretValg);
	const settValg = (nytt: Temavalg) => {
		settValgState(nytt);
		try {
			localStorage.setItem(lagringsnokkel, nytt);
		} catch {
			// localStorage kan være utilgjengelig
		}
	};
	const systemErMork = useSyncExternalStore(
		abonnerPaSystemtema,
		() => window.matchMedia(mork).matches,
	);
	const tema: Tema =
		valg === "system" ? (systemErMork ? "dark" : "light") : valg;

	// body ligger utenfor Theme, så roten må ha temaklassen for at bakgrunnen bak siden skal følge med
	useEffect(() => {
		const rot = document.documentElement;
		rot.classList.toggle("dark", tema === "dark");
		return () => rot.classList.remove("dark");
	}, [tema]);

	return { valg, tema, settValg };
}

const valgMedIkon: { verdi: Temavalg; tekst: string; ikon: ReactNode }[] = [
	{ verdi: "light", tekst: "Lyst", ikon: <SunIcon aria-hidden /> },
	{ verdi: "dark", tekst: "Mørkt", ikon: <MoonIcon aria-hidden /> },
	{ verdi: "system", tekst: "System", ikon: <MonitorIcon aria-hidden /> },
];

export function TemaBryter({
	valg,
	onVelg,
}: {
	valg: Temavalg;
	onVelg: (valg: Temavalg) => void;
}) {
	return (
		<ActionMenu>
			<Tooltip content="Bytt fargetema" placement="bottom">
				<ActionMenu.Trigger>
					<Button
						variant="secondary-neutral"
						data-color="neutral"
						size="small"
						icon={<ThemeIcon aria-hidden />}
					/>
				</ActionMenu.Trigger>
			</Tooltip>
			<ActionMenu.Content>
				<ActionMenu.Label>Velg fargetema</ActionMenu.Label>
				<ActionMenu.Group aria-label="Velg fargetema">
					{valgMedIkon.map(({ verdi, tekst, ikon }) => (
						<ActionMenu.Item
							key={verdi}
							icon={ikon}
							aria-current={valg === verdi}
							onSelect={() => onVelg(verdi)}
						>
							<HStack gap="space-24" align="center">
								{tekst}
								{valg === verdi && (
									<CheckmarkIcon aria-hidden fontSize="1.25rem" />
								)}
							</HStack>
						</ActionMenu.Item>
					))}
				</ActionMenu.Group>
			</ActionMenu.Content>
		</ActionMenu>
	);
}
