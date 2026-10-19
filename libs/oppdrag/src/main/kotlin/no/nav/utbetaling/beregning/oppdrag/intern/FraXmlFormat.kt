package no.nav.utbetaling.beregning.oppdrag.intern

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.convertValue
import tools.jackson.module.kotlin.kotlinModule

import no.nav.utbetaling.beregning.oppdrag.Bilagstype
import no.nav.utbetaling.beregning.oppdrag.DelytelseId
import no.nav.utbetaling.beregning.oppdrag.Fagområde
import no.nav.utbetaling.beregning.oppdrag.FagsystemId
import no.nav.utbetaling.beregning.oppdrag.Henvisning
import no.nav.utbetaling.beregning.oppdrag.Kidnummer
import no.nav.utbetaling.beregning.oppdrag.OppdragId
import no.nav.utbetaling.beregning.oppdrag.Oppdragstidslinje
import no.nav.utbetaling.beregning.oppdrag.Opplysning
import no.nav.utbetaling.beregning.oppdrag.SaksbehandlerId
import no.nav.utbetaling.beregning.oppdrag.StønadId
import no.nav.utbetaling.beregning.oppdrag.Utbetalingsfrekvens
import no.nav.utbetaling.beregning.oppdrag.xml.Oppdrag110
import no.nav.utbetaling.beregning.oppdrag.xml.XmlOppdrag
import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.tid.Tidslinje

internal object FraXmlFormat {
    fun opprett(oppdrag110: Oppdrag110): Oppdragstidslinje {
        val internOppdragId =
            InternOppdragId(
                fagområde = oppdrag110.kodeFagomraade.mapVerdi { Fagområde(it) } ?: error("Mangler kodeFagomraade på oppdragsnivå"),
                fagsystemId = oppdrag110.fagsystemId.mapVerdi { FagsystemId(it) } ?: error("Mangler fagsystemId på oppdragsnivå"),
            )

        val linjerPerKjede = Linjer.fra(oppdrag110)
        val kjedeendringer = kjedeendringerFra(linjerPerKjede)

        val avventinger by lazy { Avventinger.fra(oppdrag110) }
        val omposteringer by lazy { Omposteringer.fra(oppdrag110) }
        val bostedsenheter by lazy { Enheter.bostedsenheterFra(oppdrag110) }
        val behandlendeEnheter by lazy { Enheter.behandlendeEnheterFra(oppdrag110) }

        val kjeder =
            linjerPerKjede
                .mapValues { (kjede, linjer) ->
                    val statuser = kjedeendringer[kjede]?.kjedestatus ?: Tidslinje.Builder<Statusendring>().build()
                    linjer.aktive
                        // Verdier som entydig kan utledes fra verdier i linjen
                        .mapVerdier { oppdragsLinje150 ->
                            OpplysningerIntern.Builder(
                                delytelseId = oppdragsLinje150.delytelseId.mapVerdi { DelytelseId(it) } ?: error("Mangler delytelseId på linjenivå"),
                                oppdragSaksbehandlerId = oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandlerId på oppdragsnivå"),
                                linjeSaksbehandlerId = oppdragsLinje150.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandlerId på linjenivå"),
                                refDelytelseId = oppdragsLinje150.refDelytelseId.mapVerdi { DelytelseId(it) },
                                henvisning = oppdragsLinje150.henvisning.mapVerdi { Henvisning(it) },
                                sats = oppdragsLinje150.sats,
                                kidnummer = oppdragsLinje150.kid.mapVerdi { Kidnummer(it) },
                            )
                        }
                        // Periodierte verdier på linjen som er periodisert på et annet nivå enn linjen selv
                        .kombinerMed(statuser) { _, builder, statusendring ->
                            builder?.medStatus(statusendring)
                        }
                }
                // Periodiserte verdier fra oppdragsnivå som er periodisert på et annet nivå enn linjene
                .mapValues { (_, builderTidslinje) ->
                    builderTidslinje
                        .kombinerMed(avventinger) { _, builder, avventing ->
                            builder?.copy(avventingsårsakEndring = avventing)
                        }.kombinerMed(omposteringer) { _, builder, ompostering ->
                            builder?.copy(omposteringEndring = ompostering)
                        }.kombinerMed(bostedsenheter) { _, builder, bostedsenhet ->
                            builder?.copy(bostedsenhet = bostedsenhet)
                        }.kombinerMed(behandlendeEnheter) { _, builder, behandlendeEnhet ->
                            builder?.copy(behandlendeEnhet = behandlendeEnhet)
                        }.kombinerMed(GjelderId.fra(oppdrag110)) { _, builder, gjelderId ->
                            builder?.copy(gjelderId = gjelderId)
                        }
                }
                // Bygger tidslinjen med kun linjenivå-verdier; oppdragsnivå løftes ut i oppdragsverdier
                .mapValues { (_, builderTidslinje) ->
                    builderTidslinje.mapVerdier { it.build() }
                }

        return Oppdragstidslinje(
            oppdragsverdier = mapOf(internOppdragId to oppdragsverdierFra(oppdrag110)),
            kjeder = kjeder,
            oppdragsendringer = mapOf(internOppdragId to oppdragsendringerFra(oppdrag110)),
            kjedeendringer = kjedeendringer,
        )
    }

    /** JSON'en er også XML-formatet, bare i en JSON-drakt **/
    fun oppdrag110FraJson(vararg json: String) =
        json.flatMapIndexed { index, enJson ->
            val jsonNode =
                runCatching {
                    jsonMapper.readTree(enJson)
                }.getOrElse { feil -> throw IllegalArgumentException("Ugyldig JSON!", feil) }

            when {
                jsonNode.isArray -> jsonMapper.convertValue<List<Oppdrag110>>(jsonNode)
                jsonNode.isObject -> listOf(jsonMapper.convertValue<Oppdrag110>(jsonNode))
                else -> throw IllegalArgumentException("Feil på json nummer ${index + 1}: Må være JSON Objekt eller Array, var ${jsonNode.nodeType.name}")
            }
        }

    fun oppdrag110FraXml(vararg xml: String): List<Oppdrag110> {
        val xmlOppdrag =
            xml.mapIndexed { index, enXml ->
                runCatching { xmlMapper.readValue(enXml, XmlOppdrag::class.java) }.getOrElse { feil ->
                    throw IllegalArgumentException("Feil på xml nummer ${index + 1}", feil)
                }
            }

        return xmlOppdrag.mapNotNull { it.oppdrag110 }
    }

    fun fraOppdrag110(oppdrag110: List<Oppdrag110>) =
        oppdrag110.foldIndexed(Oppdragstidslinje()) { index, sammenslått, etOppdrag110 ->
            runCatching { sammenslått + FraXmlFormat.opprett(etOppdrag110) }.getOrElse { feil ->
                throw IllegalArgumentException("Feil på oppdrag nummer ${index + 1}", feil)
            }
        }

    private fun oppdragsverdierFra(oppdrag110: Oppdrag110): Oppdragsverdier {
        val oppdragSaksbehandlerId = oppdrag110.saksbehId.mapVerdi { SaksbehandlerId(it) } ?: error("Mangler saksbehandler på oppdragsnivå")
        return Oppdragsverdier(
            oppdragId = oppdrag110.oppdragsId?.let { Opplysning(oppdragSaksbehandlerId, OppdragId(it)) },
            stønadId = oppdrag110.stonadId.mapVerdi { StønadId(it) }?.let { Opplysning(oppdragSaksbehandlerId, it) },
            utbetalingsfrekvens = oppdrag110.utbetFrekvens.mapVerdi { utbetalingsfrekvenser[it] ?: error("Kjenner ikke til utbetalingsfrekvens $it") }?.let { Opplysning(oppdragSaksbehandlerId, it) },
            bilagstype =
                oppdrag110.bilagstype113
                    ?.bilagsType
                    ?.mapVerdi { Bilagstype(it) }
                    ?.let { Opplysning(oppdragSaksbehandlerId, it) },
            forfall = oppdrag110.datoForfall?.let { Opplysning(oppdragSaksbehandlerId, it) },
        )
    }

    private fun oppdragsendringerFra(oppdrag110: Oppdrag110) =
        Oppdragsendringer(
            oppdragstatus = Statuser.fra(oppdrag110),
        )

    private fun kjedeendringerFra(linjerPerKjede: Map<Kjede, Linjer>) =
        linjerPerKjede.mapValues { (_, linjer) ->
            Kjedeendringer(
                sluttdato = (linjer.aktive.lastOrNull()?.first as? Periode.LukketPeriode)?.tom,
                kjedestatus = Statuser.fra(linjer),
            )
        }

    private val utbetalingsfrekvenser =
        mapOf(
            "DAG" to Utbetalingsfrekvens.Daglig,
            "UKE" to Utbetalingsfrekvens.Ukentlig,
            "MND" to Utbetalingsfrekvens.Månedlig,
            "14DG" to Utbetalingsfrekvens.PerFjortendeDag,
            "ENG" to Utbetalingsfrekvens.Engangsutbetaling,
        )

    private val xmlMapper: ObjectMapper =
        XmlMapper
            .builder()
            .defaultUseWrapper(false)
            .addModule(SimpleModule().addDeserializer(LocalDate::class.java, LocalDateDeserializer(DateTimeFormatter.ISO_OFFSET_DATE)))
            .addModule(kotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()

    private val jsonMapper: ObjectMapper =
        JsonMapper
            .builder()
            .addModule(kotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
}
