import {
	createContext,
	type Dispatch,
	Fragment,
	type ReactNode,
	type SetStateAction,
	useCallback,
	useContext,
	useMemo,
} from "react";
import { useBeregningslenke } from "../../util/useBeregningslenke";
import { type ByggResultat, bygg, type InnsendingSkjema } from "./skjema";

type InnsendingerContextVerdi = ReturnType<typeof useBeregningslenke> & {
	innsendinger: InnsendingSkjema[];
	setInnsendinger: Dispatch<SetStateAction<InnsendingSkjema[]>>;
	resultat: ByggResultat;
};

const InnsendingerContext = createContext<InnsendingerContextVerdi | undefined>(
	undefined,
);

export function InnsendingerProvider({ children }: { children: ReactNode }) {
	const lenketilstand = useBeregningslenke();
	const { skjema, settSkjema } = lenketilstand;
	const { innsendinger } = skjema;
	const setInnsendinger = useCallback(
		(oppdater: SetStateAction<InnsendingSkjema[]>) =>
			settSkjema((forrige) => ({
				...forrige,
				innsendinger:
					typeof oppdater === "function"
						? oppdater(forrige.innsendinger)
						: oppdater,
			})),
		[settSkjema],
	);
	const resultat = useMemo(() => bygg(innsendinger), [innsendinger]);
	const verdi = { ...lenketilstand, innsendinger, setInnsendinger, resultat };

	return (
		<InnsendingerContext.Provider value={verdi}>
			<Fragment key={lenketilstand.gjenoppretting}>{children}</Fragment>
		</InnsendingerContext.Provider>
	);
}

export function useInnsendinger(): InnsendingerContextVerdi {
	const context = useContext(InnsendingerContext);
	if (context === undefined) {
		throw new Error("useInnsendinger må brukes innenfor InnsendingerProvider");
	}
	return context;
}
