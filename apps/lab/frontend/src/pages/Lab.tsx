import { Button, Heading, Textarea } from "@navikt/ds-react";
import { isAxiosError } from "axios";
import { type SubmitEvent, useState } from "react";
import { postTolkOppdrag } from "../api/apiService";
import { HttpStatusCodeError } from "../types/Error";
import styles from "./Lab.module.css";

export default function Laboratorium() {
	const [xmlTekst, settXmlTekst] = useState("");
	const [resultat, settResultat] = useState<string | null>(null);
	const [respons, settRespons] = useState<string | null>(null);
	const [sender, settSender] = useState(false);
	const [feltFeil, settFeltFeil] = useState<string | null>(null);

	const handterInnsending = async (hendelse: SubmitEvent<HTMLFormElement>) => {
		hendelse.preventDefault();
		if (sender) return;

		settResultat(null);
		settRespons(null);
		if (!xmlTekst.trim()) {
			settFeltFeil("Skriv inn et XML-oppdrag.");
			return;
		}
		settFeltFeil(null);
		settSender(true);

		try {
			settRespons(await postTolkOppdrag(xmlTekst));
			settResultat("XML-en er behandlet.");
		} catch (error) {
			if (isAxiosError<string>(error) && error.response) {
				settRespons(error.response.data);
			}
			settResultat(
				error instanceof HttpStatusCodeError ||
					(isAxiosError(error) && error.response)
					? "Kunne ikke behandle XML-en. Kontroller innholdet og prøv igjen."
					: "Kunne ikke behandle XML-en. Prøv igjen.",
			);
		} finally {
			settSender(false);
		}
	};

	return (
		<main className={styles.laboratorium}>
			<section
				className={styles.laboratorium__panel}
				aria-labelledby="lab-heading"
			>
				<Heading
					className={styles.laboratorium__overskrift}
					id="lab-heading"
					level="1"
					size="large"
				>
					Lab verktøy
				</Heading>
				<form
					className={styles.laboratorium__skjema}
					onSubmit={handterInnsending}
					aria-busy={sender}
				>
					<Textarea
						className={styles.laboratorium__xmlfelt}
						label="XML-oppdrag"
						description="Bruk bare syntetiske XML-data. Ikke lim inn personopplysninger eller reelle ID-er."
						minRows={16}
						readOnly={sender}
						error={feltFeil}
						value={xmlTekst}
						onChange={(hendelse) => {
							settXmlTekst(hendelse.target.value);
							settResultat(null);
							settFeltFeil(null);
						}}
					/>
					<Button type="submit" loading={sender}>
						Behandle XML
					</Button>
					{resultat && (
						<p className={styles.laboratorium__status} role="status">
							{resultat}
						</p>
					)}
				</form>
				{respons !== null && (
					<section aria-labelledby="respons-heading">
						<h2 id="respons-heading">Respons fra backend</h2>
						<pre className={styles.laboratorium__respons}>{respons}</pre>
					</section>
				)}
			</section>
		</main>
	);
}
