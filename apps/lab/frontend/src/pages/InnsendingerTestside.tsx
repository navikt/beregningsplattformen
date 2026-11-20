import { BodyShort, Box, Heading, LocalAlert, VStack } from "@navikt/ds-react";
import Breadcrumbs from "../components/Breadcrumbs";
import { InnsendingerSkjema } from "../components/innsendinger/InnsendingerSkjema";

const tittelId = "innsendinger-tittel";

// Samme oppbygging som «Send inn oppdrags-XML», men med overskrift og brødsmuler i et header-landemerke
export default function InnsendingerTestside() {
	return (
		<>
			<Box as="header" width="100%">
				<Box asChild marginBlock="space-24">
					<Heading id={tittelId} level="1" size="large" align="center">
						Bygg innsendinger
					</Heading>
				</Box>
				<Breadcrumbs gjeldendeSide="Bygg innsendinger" />
			</Box>
			<VStack
				as="main"
				aria-labelledby={tittelId}
				align="center"
				padding="space-32"
				width="100%"
			>
				<Box
					width="100%"
					maxWidth="90rem"
					padding="space-32"
					borderWidth="1"
					borderColor="neutral-subtle"
					borderRadius="12"
					background="meta-purple-moderateA"
				>
					<VStack gap="space-16">
						<BodyShort>
							Bygg én eller flere innsendinger med oppdrag, kjeder og linjer, og
							kopier XML-en som lages underveis.
						</BodyShort>
						<LocalAlert status="warning">
							<LocalAlert.Header>
								<LocalAlert.Title>Bruk bare syntetiske data</LocalAlert.Title>
							</LocalAlert.Header>
							<LocalAlert.Content>
								Ikke skriv inn personopplysninger eller reelle ID-er.
							</LocalAlert.Content>
						</LocalAlert>
						<InnsendingerSkjema />
					</VStack>
				</Box>
			</VStack>
		</>
	);
}
