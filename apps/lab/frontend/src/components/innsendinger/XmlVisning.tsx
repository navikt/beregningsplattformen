export function XmlVisning({ xml }: { xml: string }) {
	return (
		<>
			{Array.from(xml.matchAll(/<[^>]+>|[^<]+/g), ({ 0: delString, index }) =>
				delString.startsWith("<") ? (
					<span key={index} style={{ color: "var(--ax-text-warning-subtle)" }}>
						{delString}
					</span>
				) : (
					delString
				),
			)}
		</>
	);
}
