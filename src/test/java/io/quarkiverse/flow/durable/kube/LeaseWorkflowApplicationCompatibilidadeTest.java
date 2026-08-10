package io.quarkiverse.flow.durable.kube;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkiverse.flow.durable.kube.config.LeaseGroupConfig;
import io.serverlessworkflow.impl.WorkflowApplication;
import jakarta.enterprise.event.Event;

class LeaseWorkflowApplicationCompatibilidadeTest {

    @Test
    void vinculaNomeDaLeaseDeMembroAoIdDaWorkflowApplication() {
        var leaseName = "flow-pool-member-conformidade-00";
        var customizer = new InjectLeaseWorkflowApplicationBuilderCustomizer();
        var memberLease = mock(LeaseGroupConfig.LeaseConfig.class);
        customizer.leaseConfig = mock(LeaseGroupConfig.class);
        customizer.devModeStrategy = mock(DevModeStrategy.class);
        customizer.memberLeaseCoordinator = mock(MemberLeaseCoordinator.class);
        customizer.leaseStartupEvent = eventMock();
        var builder = mock(WorkflowApplication.Builder.class);

        when(customizer.leaseConfig.member()).thenReturn(memberLease);
        when(memberLease.enabled()).thenReturn(true);
        when(customizer.devModeStrategy.enabled()).thenReturn(false);
        when(customizer.memberLeaseCoordinator.awaitLease(Duration.ofSeconds(30))).thenReturn(leaseName);

        customizer.customize(builder);

        verify(customizer.leaseStartupEvent).fire(any(LeaseStartupEvent.class));
        verify(builder).withId(leaseName);
    }

    @SuppressWarnings("unchecked")
    private static Event<LeaseStartupEvent> eventMock() {
        return mock(Event.class);
    }
}
