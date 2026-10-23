import "@navikt/ds-css";
import { FileCodeIcon } from "@navikt/aksel-icons";
import { Box, Heading, LinkCard } from "@navikt/ds-react";
import React from "react";
import ReactDOM from "react-dom/client";
import {
	BrowserRouter,
	Route,
	Link as RouterLink,
	Routes,
} from "react-router-dom";
import styles from "./App.module.css";
import Breadcrumbs from "./components/Breadcrumbs";
import SendInnOppdragsXML from "./pages/SendInnOppdragsXML";
import { initApm } from "./util/apm";

initApm();

function App() {
	return (
		<BrowserRouter>
			<div className={styles["app__page-wrapper"]}>
				<div className={styles["app__page-layout"]}>
					<Routes>
						<Route path="/" element={<LabMenu />} />
						<Route
							path="/send-inn-oppdrags-xml"
							element={
								<>
									<Heading
										className={styles.app__heading}
										id="lab-heading"
										level="1"
										size="large"
									>
										Send inn Oppdrags-XML
									</Heading>
									<Breadcrumbs />
									<SendInnOppdragsXML />
								</>
							}
						/>
						<Route
							path="*"
							element={
								<main>
									<Heading level="1" size="large">
										Siden ble ikke funnet
									</Heading>
									<RouterLink to="/">Tilbake til menyen</RouterLink>
								</main>
							}
						/>
					</Routes>
				</div>
			</div>
		</BrowserRouter>
	);
}

function LabMenu() {
	return (
		<main>
			<header className={styles.menu__header}>
				<Heading level="1" size="large">
					Meny
				</Heading>
			</header>
			<div className={styles.menu}>
				<div className={styles.menu__cards}>
					<LinkCard>
						<Box
							asChild
							borderRadius="12"
							padding="space-8"
							className={styles["menu__icon-box"]}
						>
							<LinkCard.Icon>
								<FileCodeIcon
									className={styles.menu__icon}
									aria-hidden="true"
								/>
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
				</div>
			</div>
		</main>
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
