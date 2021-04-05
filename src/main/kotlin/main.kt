import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import generated.Projects
import generated.projects.Issue
import generated.projects.PullRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.headers
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.lang.NumberFormatException
import java.net.URL

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("App")

    val parser = ArgParser("default")
    val login by parser
        .option(
            ArgType.String,
            shortName = "l",
            description = "GitHub organization name.",
        )
        .required()

    val number by parser
        .option(
            ArgType.Int,
            shortName = "n",
            description = "GitHub projects number.",
        )
        .required()

    val token by parser
        .option(
            ArgType.String,
            shortName = "t",
            description = """
                GitHub API's token(default: use environment variable(ORG_GRADLE_PROJECT_githubToken).
                More information: https://docs.github.com/ja/graphql/guides/forming-calls-with-graphql
                """
                .trimIndent(),
        )

    val verbose by parser
        .option(
            ArgType.Boolean,
            shortName = "v",
        )

    parser.parse(args)

    val isDebug = verbose ?: false

    val githubToken = token
        ?: System.getenv("ORG_GRADLE_PROJECT_githubToken")
        ?: ""

    if (githubToken.isEmpty()) {
        log.error("GitHub API token is blank. Set the value in one of the following ways. Show detail option help(-h).")
        return
    }

    val client = GraphQLKtorClient(
        url = URL("https://api.github.com/graphql"),
        serializer = GraphQLClientJacksonSerializer(),
        httpClient = HttpClient(engineFactory = Apache) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = if (isDebug) LogLevel.ALL else LogLevel.NONE
            }
        }
    )

    runBlocking {
        val result = client.execute(Projects(Projects.Variables(login, number))) {
            headers {
                append("Authorization", "bearer $githubToken")
            }
        }

        if (isDebug) {
            log.debug("GraphQL Result: $result")
        }

        val pullRequests = result.data?.viewer?.organization?.project?.columns?.edges ?: emptyList()

        val cards = pullRequests
            .filter {
                it?.node?.name == "Closed"
            }
            .mapNotNull {
                it?.node?.cards?.edges
            }
            .flatten()

        val labels = cards
            .mapNotNull {
                it?.node?.content
            }
            .mapNotNull {
                when (it) {
                    is Issue -> it.labels?.edges
                    is PullRequest -> it.labels?.edges
                    else -> null
                }
            }
            .flatten()

        val sum = labels
            .mapNotNull {
                it?.node?.name
            }
            .map {
                try {
                    Integer.parseInt(it)
                } catch (_e: NumberFormatException) {
                    0
                }
            }.sum()

        println(sum)
    }
}
