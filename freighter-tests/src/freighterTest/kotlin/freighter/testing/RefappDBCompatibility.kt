package freighter.testing

import com.r3.refapp.flows.CreateCustomerFlow
import com.r3.refapp.schemas.TransactionType
import freighter.deployments.DeploymentContext
import freighter.deployments.NodeBuilder
import freighter.deployments.SingleNodeDeployment
import freighter.deployments.UnitOfDeployment
import freighter.installers.corda.ENTERPRISE
import freighter.machine.DeploymentMachineProvider
import freighter.machine.NodeMachine
import freighter.machine.generateRandomString
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.InputStreamAndHash
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import utility.getOrThrow
import java.io.File
import java.time.Duration
import java.util.*

@Execution(ExecutionMode.CONCURRENT)
class RefappDBCompatibility : DockerRemoteMachineBasedTest() {

    val gradleProperties = Properties()
    val constants = Properties()

    init {
        gradleProperties.load(FileUtils.openInputStream(File("..${File.separator}gradle.properties")))
        constants.load(FileUtils.openInputStream(File("..${File.separator}constants.properties")))
    }

    val refappContracts =
            NodeBuilder.DeployedCordapp.fromClassPath("contracts-${gradleProperties.getProperty("version")}")

    val refappWorkflows =
            NodeBuilder.DeployedCordapp.fromClassPath("workflows-${gradleProperties.getProperty("version")}")

    val stressTesterCordapp = NodeBuilder.DeployedCordapp.fromClassPath("freighter-cordapp-flows")

    @Test
    fun `refapp can be loaded on a node running postgres 9_6`() {
        runRefappOnNodeRunningDatabase(DeploymentMachineProvider.DatabaseType.PG_9_6)
    }

    @Test
    fun `refapp can be loaded on a node running postgres 10_10`() {
        runRefappOnNodeRunningDatabase(DeploymentMachineProvider.DatabaseType.PG_10_10)
    }

    @Test
    fun `refapp can be loaded on a node running postgres 11_5`() {
        runRefappOnNodeRunningDatabase(DeploymentMachineProvider.DatabaseType.PG_11_5)
    }

    @Test
    @AzureTest
    fun `refapp can be loaded on a node running ms_sql`() {
        runRefappOnNodeRunningDatabase(DeploymentMachineProvider.DatabaseType.MS_SQL)
    }

    @Test
    @OracleTest
    fun `refapp can be loaded on a node running oracle 12 r2`() {
        runRefappOnNodeRunningDatabase(DeploymentMachineProvider.DatabaseType.ORACLE_12_R2)
    }

    private fun runRefappOnNodeRunningDatabase(db: DeploymentMachineProvider.DatabaseType) {
        val randomString = generateRandomString()

        val deploymentContext = DeploymentContext(machineProvider, nms, artifactoryUsername, artifactoryPassword)

        val deploymentResult = SingleNodeDeployment(
                NodeBuilder().withX500("O=PartyB, C=GB, L=LONDON, CN=$randomString")
                        .withCordapp(stressTesterCordapp)
                        .withCordapp(refappContracts)
                        .withCordapp(refappWorkflows)
                        .withDatabase(machineProvider.requestDatabase(db))
        ).withVersion(UnitOfDeployment.CORDA_4_6).withDistribution(ENTERPRISE)
                .deploy(deploymentContext)

        val nodeMachine = deploymentResult.getOrThrow().nodeMachines.single()

        verifyCustomerAndAttachmentTables(nodeMachine)
        verifyAccountSchemaTable(nodeMachine)
        veifyTransactionLogTable(nodeMachine)
        verifyRecurringPaymentsTables(nodeMachine)
    }

    private fun verifyCustomerAndAttachmentTables(nodeMachine: NodeMachine) {
        nodeMachine.rpc {

            val customerName: String = "test customer"
            val contactNumber: String = "123456789"
            val emailAddress: String = "test.email@r3.com"
            val postCode: String = "QWERTY1"

            val hash = uploadAttachment(InputStreamAndHash.createInMemoryTestZip(1024, 0).inputStream)
            val attachments: List<Pair<SecureHash, String>> = listOf(Pair(hash, "attachment"))

            val createdCustomer = startFlow(
                    ::CreateCustomerFlow, customerName, contactNumber, emailAddress, postCode, attachments)
                    .returnValue.getOrThrow(Duration.ofSeconds(30))
            println("Successfully created customer: $createdCustomer")
        }
    }

    private fun veifyTransactionLogTable(nodeMachine: NodeMachine) {
        nodeMachine.withDB {
            val uuid = UUID.randomUUID().toString()
            val txType = TransactionType.DEPOSIT
            // txid, account from, account to, amount, currency, txDate, txType
            it.prepareStatement("INSERT INTO transaction_log VALUES ('$uuid', '$uuid', '$uuid', 1000, 'EUR', '2020-07-03T14:47:00Z', '$txType')").execute()
            val rs = it.prepareStatement("SELECT count(*) from transaction_log where tx_id = '$uuid'").executeQuery()
            rs.next()
            assertEquals(1, rs.getInt("count"))
        }
    }

    private fun verifyAccountSchemaTable(nodeMachine: NodeMachine) {
        nodeMachine.withDB {
            val uuid = UUID.randomUUID().toString()

            // customer_id, created_on, modified_on, customer_name, contact_number, email, postcode
            it.prepareStatement("INSERT INTO customer VALUES ('$uuid', '2020-07-03T14:47:00Z', '2020-07-03T14:47:00Z', 'customer name', '123456789', 'test_email@r3.com', '123QWERTY')").execute()

            // txid, output index, account id, balance, txDate, status, customer id,  linear id, withdrawal limit, transfer limit, overdraft balance, overdraft limit
            it.prepareStatement("INSERT INTO account_schema VALUES ('txid1234567890', 1, '$uuid', 10, '2020-07-03T14:47:00Z', 'ACTIVE', '$uuid', '$uuid', 100, 1000, 0, 0)").execute()
            val rs = it.prepareStatement("SELECT count(*) from account_schema where transaction_id = 'txid1234567890'").executeQuery()
            rs.next()
            assertEquals(1, rs.getInt("count"))
        }
    }

    private fun verifyRecurringPaymentsTables(nodeMachine: NodeMachine) {
        nodeMachine.withDB {
            val uuidFrom = UUID.randomUUID().toString()
            val uuidTo = UUID.randomUUID().toString()
            val linearId = UUID.randomUUID().toString()
            val txId = "txid1234567890"

            // txid, output index, account from, account to, amount, currency, date start, period, iteration num,
            // linear id
            it.prepareStatement("INSERT INTO recurring_payment VALUES ('$txId', 1, '$uuidFrom', '$uuidTo', 1000, 'EUR', " +
                    "'2020-07-03T14:47:00Z', 10000000, NULL, '$linearId')").execute()
            val rs = it.prepareStatement("SELECT count(*) from recurring_payment where transaction_id = 'txid1234567890'").executeQuery()
            rs.next()
            assertEquals(1, rs.getInt("count"))

            // log id, txDate, error, rp tx, rp idx
            it.prepareStatement("INSERT INTO recurring_payment_log VALUES ('test12222', '2020-07-03T14:47:00Z', 'error', '$txId', 1)").execute()
            val rsLog = it.prepareStatement("SELECT count(*) from recurring_payment_log where log_id = 'test12222'").executeQuery()
            rsLog.next()
            assertEquals(1, rsLog.getInt("count"))
        }
    }
}