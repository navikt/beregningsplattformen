import {
	createContext,
	type Dispatch,
	type ReactNode,
	type SetStateAction,
	useContext,
	useMemo,
	useState,
} from "react";
import { standardInnsending } from "./fraXml";
import { type ByggResultat, bygg, type InnsendingSkjema } from "./skjema";

type InnsendingerContextVerdi = {
	innsendinger: InnsendingSkjema[];
	setInnsendinger: Dispatch<SetStateAction<InnsendingSkjema[]>>;
	resultat: ByggResultat;
};

const InnsendingerContext = createContext<InnsendingerContextVerdi | undefined>(
	undefined,
);

export function InnsendingerProvider({ children }: { children: ReactNode }) {
	const [innsendinger, setInnsendinger] = useState<InnsendingSkjema[]>(() => [
		standardInnsending(),
	]);
	const resultat = useMemo(() => bygg(innsendinger), [innsendinger]);
	const verdi = useMemo(
		() => ({ innsendinger, setInnsendinger, resultat }),
		[innsendinger, resultat],
	);

	return (
		<InnsendingerContext.Provider value={verdi}>
			{children}
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
