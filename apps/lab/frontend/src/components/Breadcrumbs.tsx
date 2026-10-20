import { ChevronRightDoubleIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import { Link as RouterLink } from "react-router-dom";
import styles from "./Breadcrumbs.module.css";

export default function Breadcrumbs() {
	return (
		<nav className={styles.breadcrumbs} aria-label="Brødsmulesti">
			<div className={styles.breadcrumbs__bar}>
				<ol className={styles.breadcrumbs__items}>
					<li>
						<Link as={RouterLink} to="/">
							Tilbake til menyen
						</Link>
					</li>
					<li className={styles.breadcrumbs__current}>
						<ChevronRightDoubleIcon aria-hidden />
						<span aria-current="page">Send inn oppdrags-XML</span>
					</li>
				</ol>
			</div>
		</nav>
	);
}
