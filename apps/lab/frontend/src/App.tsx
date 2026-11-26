import "@navikt/ds-css";
import { FileCodeIcon } from "@navikt/aksel-icons";
import {
	Box,
	Heading,
	HStack,
	Link,
	LinkCard,
	Page,
	Theme,
} from "@navikt/ds-react";
import React from "react";
import ReactDOM from "react-dom/client";
import {
	BrowserRouter,
	Route,
	Link as RouterLink,
	Routes,
} from "react-router-dom";
import Breadcrumbs from "./components/Breadcrumbs";
import { TemaBryter, useTema } from "./components/TemaBryter";
import SendInnOppdragsXML from "./pages/send-inn-oppdrags-xml/SendInnOppdragsXML";
import { initApm } from "./util/apm";
import "./App.css";

initApm();

function App() {
	const { valg, tema, settValg } = useTema();
	return (
		<BrowserRouter>
			<Theme theme={tema} hasBackground={false}>
				<Page>
					<HStack justify="end" padding="space-8">
						<TemaBryter valg={valg} onVelg={settValg} />
					</HStack>
					<Routes>
						<Route path="/" element={<LabMenu />} />
						<Route
							path="/send-inn-oppdrags-xml"
							element={
								<Page.Block width="2xl">
									<Heading
										id="lab-heading"
										level="1"
										size="large"
										align="center"
										spacing
									>
										Send inn Oppdrags-XML
									</Heading>
									<Breadcrumbs />
									<SendInnOppdragsXML />
								</Page.Block>
							}
						/>
						<Route
							path="*"
							element={
								<Page.Block width="text" gutters>
									<Heading level="1" size="large">
										Siden ble ikke funnet
									</Heading>

									<Link as={RouterLink} to="/">
										Tilbake til menyen
									</Link>
								</Page.Block>
							}
						/>
					</Routes>
				</Page>
			</Theme>
		</BrowserRouter>
	);
}

function LabMenu() {
	return (
		<Page.Block width="text" gutters>
			<Heading level="1" size="large" spacing align="center">
				Meny
			</Heading>
			<LinkCard>
				<Box asChild borderRadius="12" padding="space-8" background="moderateA">
					<LinkCard.Icon>
						<FileCodeIcon aria-hidden="true" fontSize="2rem" />
					</LinkCard.Icon>
				</Box>
				<LinkCard.Title as="h2">
					<LinkCard.Anchor asChild>
						<RouterLink to="/send-inn-oppdrags-xml">
							Send inn oppdrags-XML
						</RouterLink>
					</LinkCard.Anchor>
				</LinkCard.Title>
				<LinkCard.Description>
					Bygg og send inn et oppdrag i XML-format.
				</LinkCard.Description>
			</LinkCard>
		</Page.Block>
	);
}

const startMsw = async () => {
	if (import.meta.env.MODE === "mock") {
		try {
			const { worker } = await import("../mock/browser");

			await worker.start({
				serviceWorker: {
					url: "/mockServiceWorker.js",
				},
				quiet: false,
			});
		} catch (error) {
			// biome-ignore lint/suspicious/noConsole: debug code
			console.error("Failed to start MSW", error);
		}
	}
};

startMsw().then(() => {
	ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
		<React.StrictMode>
			<App />
		</React.StrictMode>,
	);
});
