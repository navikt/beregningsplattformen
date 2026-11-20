import { Heading, Link, VStack } from "@navikt/ds-react";
import { Link as RouterLink } from "react-router-dom";
import { InnsendingerSkjema } from "../components/innsendinger/InnsendingerSkjema";

export default function InnsendingerTestside() {
	return (
		<main>
			<VStack gap="space-24">
				<Link as={RouterLink} to="/">
					Tilbake til menyen
				</Link>
				<Heading level="1" size="large">
					Bygg innsendinger
				</Heading>
				<InnsendingerSkjema />
			</VStack>
		</main>
	);
}
