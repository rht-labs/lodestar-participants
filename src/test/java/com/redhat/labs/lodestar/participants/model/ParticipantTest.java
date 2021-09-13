package com.redhat.labs.lodestar.participants.model;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ParticipantTest {

    @Test
    void testSame() {
        Participant p = Participant.builder().build();
        
        Participant o = Participant.builder().email("o@com.com").build();
        
        assertFalse(p.isSame(o));
        
        p.setFirstName("p");
        
        assertFalse(p.isSame(o));
        assertFalse(o.isSame(p));

        o.setFirstName("p");
        p.setEmail("o@com.com");

        p.setLastName("p");
        o.setLastName("p");

        assertTrue(p.isSame(o));

        p.setRole("g");
        assertFalse(p.isSame(o));

        o.setRole("g");
        assertTrue(p.isSame(o));

    }

    @Test
    void testNeedsReset() {
        Participant p = Participant.builder().reset(false).build();

        assertFalse(p.needsReset());
    }
}
