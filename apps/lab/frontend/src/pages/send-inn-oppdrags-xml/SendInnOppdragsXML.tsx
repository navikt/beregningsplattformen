import { Box, Button, HStack, VStack } from "@navikt/ds-react";
import { type SubmitEvent, useRef, useState } from "react";
import { postBeregn } from "../../api/apiService";
import { LaStaaResultater } from "./components/LaStaaResultater";
import {
	FellesBeregningsfelt,
	type FellesFeltfeil,
} from "../../components/FellesBeregningsfelt";
import { Innsendingstidslinje } from "./components/Innsendingstidslinje";
import {
	InnsendingerProvider,
	useInnsendinger,
} from "../../components/innsendinger/InnsendingerContext";
import { InnsendingerSkjema } from "../../components/innsendinger/InnsendingerSkjema";
import { SkattekortSkjema } from "../../components/SkattekortSkjema";
import { SkjemalenkeMeldinger } from "../../components/SkjemalenkeMeldinger";
import { TrekkSkjema } from "../../components/TrekkSkjema";
import type { BeregningResponse } from "../../types/BeregningResponse";
import { HttpStatusCodeError } from "../../types/Error";
import { lesSkattekort } from "../../types/Skattekort";
import { lesTrekk, type TrekkFeil } from "../../types/Trekk";

export default function SendInnOppdragsXML() {
	return (
		<InnsendingerProvider>
			<SendInnOppdragsXMLInnhold />
		</InnsendingerProvider>
	);
}

function SendInnOppdragsXMLInnhold() {
	const {
		skjema,
		settSkjema,
		nullstilt,
		lagringsfeil,
		bekreftNullstilling,
		resultat,
	} = useInnsendinger();
	const { fellesOpplysninger, skattekortUtkast, trekkUtkast } = skjema;
	const [fellesfeil, settFellesfeil] = useState<FellesFeltfeil[]>([]);
	const [trekkfeil, settTrekkfeil] = useState<TrekkFeil[]>([]);
	const senderRef = useRef(false);
	const [sender, settSender] = useState(false);
	const [feilmelding, settFeilmelding] = useState<string | null>(null);
	const [respons, settRespons] = useState<BeregningResponse | null>(null);
	const fjernResultat = () => {
		settFeilmelding(null);
		settRespons(null);
	};

	const handterInnsending = async (hendelse: SubmitEvent<HTMLFormElement>) => {
		hendelse.preventDefault();
		if (senderRef.current) return;

		fjernResultat();
		const { fom, tom, fomGyldig, tomGyldig, beregningsTidspunkt } =
			fellesOpplysninger;
		const feltfeil: FellesFeltfeil[] = [];
		if (!fomGyldig) {
			feltfeil.push({ felt: "fom", melding: "Oppgi en gyldig fra-dato." });
		}
		if (!tomGyldig) {
			feltfeil.push({ felt: "tom", melding: "Oppgi en gyldig til-dato." });
		} else if (fom && tom && fom > tom) {
			feltfeil.push({
				felt: "tom",
				melding: "Til-datoen må være lik eller etter fra-datoen.",
			});
		}
		settFellesfeil(feltfeil);
		const trekkResultat = lesTrekk(trekkUtkast);
		settTrekkfeil(trekkResultat.ok ? [] : trekkResultat.feil);
		if (feltfeil.length > 0 || !trekkResultat.ok) return;

		senderRef.current = true;
		settSender(true);
		try {
			const response = await postBeregn({
				xml: resultat.innsendinger
					.map((innsending) => innsending.xml)
					.filter((xml) => xml !== undefined),
				...(fom ? { fom } : {}),
				...(tom ? { tom } : {}),
				...(beregningsTidspunkt ? { beregningsTidspunkt } : {}),
				...(skattekortUtkast.aktiv
					? { skattekort: lesSkattekort(skattekortUtkast) }
					: {}),
				trekk: trekkResultat.trekk,
			});
			settRespons(response);
		} catch (error) {
			if (error instanceof HttpStatusCodeError && error.apiMessage) {
				settFeilmelding(error.apiMessage);
			}
		} finally {
			senderRef.current = false;
			settSender(false);
		}
	};

	return (
		<VStack align="center" gap="space-32" marginBlock="space-32">
			<SkjemalenkeMeldinger
				nullstilt={nullstilt}
				lagringsfeil={lagringsfeil}
				bekreftNullstilling={bekreftNullstilling}
			/>
			<Box
				background="default"
				borderRadius="12"
				borderWidth="1"
				borderColor="neutral-subtle"
				padding="space-16"
				asChild
			>
				<VStack gap="space-24" width="100%" asChild>
					<form onSubmit={handterInnsending} aria-busy={sender}>
						<InnsendingerSkjema />

						<SkattekortSkjema
							utkast={skattekortUtkast}
							sender={sender}
							onChange={(utkast) => {
								fjernResultat();
								settSkjema((verdi) => ({ ...verdi, skattekortUtkast: utkast }));
							}}
						/>

						<TrekkSkjema
							utkast={trekkUtkast}
							feil={trekkfeil}
							sender={sender}
							onChange={(oppdater) => {
								fjernResultat();
								settTrekkfeil([]);
								settSkjema((verdi) => ({
									...verdi,
									trekkUtkast: oppdater(verdi.trekkUtkast),
								}));
							}}
						/>

						<FellesBeregningsfelt
							verdi={fellesOpplysninger}
							feil={fellesfeil}
							sender={sender}
							onChange={(endringer) => {
								fjernResultat();
								settFellesfeil([]);
								settSkjema((verdi) => ({
									...verdi,
									fellesOpplysninger: {
										...verdi.fellesOpplysninger,
										...endringer,
									},
								}));
							}}
						/>

						<HStack justify="end">
							<Button type="submit" variant="primary" size="medium">
								Send inn
							</Button>
						</HStack>
					</form>
				</VStack>
			</Box>

			<Innsendingstidslinje feilmelding={feilmelding} respons={respons} />

			<LaStaaResultater
				gjeldende={respons?.beregningstidslinje?.beregninger ?? []}
				lagret={skjema.lagretResultat}
				onChange={(lagretResultat) =>
					settSkjema((verdi) => ({ ...verdi, lagretResultat }))
				}
			/>
		</VStack>
	);
}
