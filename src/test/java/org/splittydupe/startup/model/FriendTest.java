package org.splittydupe.startup.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Friend Model Tests")
class FriendTest {

    @Test
    @DisplayName("Should create Friend with builder")
    void shouldCreateFriendWithBuilder() {
        Friend friend = Friend.builder()
                .id("friend-123")
                .userId("user-456")
                .displayName("John Doe")
                .venmoHandle("@johndoe")
                .zellePhoneNumber("+15551234567")
                .paypalHandle("john.doe@email.com")
                .build();

        assertEquals("friend-123", friend.getId());
        assertEquals("user-456", friend.getUserId());
        assertEquals("John Doe", friend.getDisplayName());
        assertEquals("@johndoe", friend.getVenmoHandle());
        assertEquals("+15551234567", friend.getZellePhoneNumber());
        assertEquals("john.doe@email.com", friend.getPaypalHandle());
    }

    @Test
    @DisplayName("Should create Friend with no-args constructor")
    void shouldCreateFriendWithNoArgsConstructor() {
        Friend friend = new Friend();
        friend.setId("friend-789");
        friend.setUserId("user-111");
        friend.setDisplayName("Jane Smith");

        assertEquals("friend-789", friend.getId());
        assertEquals("user-111", friend.getUserId());
        assertEquals("Jane Smith", friend.getDisplayName());
    }

    @Test
    @DisplayName("Should create Friend with all-args constructor")
    void shouldCreateFriendWithAllArgsConstructor() {
        Friend friend = new Friend(
                "friend-001",
                "user-002",
                "Alice Johnson",
                "@alicej",
                "+15559876543",
                "alice.j@email.com",
                "alice.johnson@example.com"
        );

        assertEquals("friend-001", friend.getId());
        assertEquals("user-002", friend.getUserId());
        assertEquals("Alice Johnson", friend.getDisplayName());
        assertEquals("@alicej", friend.getVenmoHandle());
        assertEquals("+15559876543", friend.getZellePhoneNumber());
        assertEquals("alice.j@email.com", friend.getPaypalHandle());
        assertEquals("alice.johnson@example.com", friend.getContactEmail());
    }

    @Test
    @DisplayName("Should handle Friend with only Venmo")
    void shouldHandleFriendWithOnlyVenmo() {
        Friend friend = Friend.builder()
                .id("friend-v")
                .userId("user-1")
                .displayName("Bob Brown")
                .venmoHandle("@bobbrown")
                .build();

        assertEquals("@bobbrown", friend.getVenmoHandle());
        assertNull(friend.getZellePhoneNumber());
        assertNull(friend.getPaypalHandle());
    }

    @Test
    @DisplayName("Should handle Friend with only Zelle")
    void shouldHandleFriendWithOnlyZelle() {
        Friend friend = Friend.builder()
                .id("friend-z")
                .userId("user-2")
                .displayName("Carol White")
                .zellePhoneNumber("+15551112222")
                .build();

        assertNull(friend.getVenmoHandle());
        assertEquals("+15551112222", friend.getZellePhoneNumber());
        assertNull(friend.getPaypalHandle());
    }

    @Test
    @DisplayName("Should handle Friend with only PayPal")
    void shouldHandleFriendWithOnlyPayPal() {
        Friend friend = Friend.builder()
                .id("friend-p")
                .userId("user-3")
                .displayName("David Green")
                .paypalHandle("david.green@email.com")
                .build();

        assertNull(friend.getVenmoHandle());
        assertNull(friend.getZellePhoneNumber());
        assertEquals("david.green@email.com", friend.getPaypalHandle());
    }

    @Test
    @DisplayName("Should handle Friend with all payment methods")
    void shouldHandleFriendWithAllPaymentMethods() {
        Friend friend = Friend.builder()
                .id("friend-all")
                .userId("user-4")
                .displayName("Emma Black")
                .venmoHandle("@emmablack")
                .zellePhoneNumber("+15553334444")
                .paypalHandle("emma.black@email.com")
                .build();

        assertNotNull(friend.getVenmoHandle());
        assertNotNull(friend.getZellePhoneNumber());
        assertNotNull(friend.getPaypalHandle());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend3 = Friend.builder()
                .id("friend-2")
                .userId("user-2")
                .displayName("Jane Smith")
                .build();

        assertEquals(friend1, friend2);
        assertNotEquals(friend1, friend3);
        assertEquals(friend1.hashCode(), friend2.hashCode());
    }

    @Test
    @DisplayName("Should handle equals with different id")
    void shouldHandleEqualsWithDifferentId() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-2")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different userId")
    void shouldHandleEqualsWithDifferentUserId() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-2")
                .displayName("John Doe")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different displayName")
    void shouldHandleEqualsWithDifferentDisplayName() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("Jane Doe")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different contactEmail")
    void shouldHandleEqualsWithDifferentContactEmail() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .contactEmail("john@example.com")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .contactEmail("jane@example.com")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different venmoHandle")
    void shouldHandleEqualsWithDifferentVenmoHandle() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .venmoHandle("@john1")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .venmoHandle("@john2")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different zellePhoneNumber")
    void shouldHandleEqualsWithDifferentZellePhoneNumber() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .zellePhoneNumber("+15551111111")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .zellePhoneNumber("+15552222222")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with different paypalHandle")
    void shouldHandleEqualsWithDifferentPaypalHandle() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .paypalHandle("john1@email.com")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .paypalHandle("john2@email.com")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with null fields")
    void shouldHandleEqualsWithNullFields() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .build();

        assertEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with null vs non-null fields")
    void shouldHandleEqualsWithNullVsNonNullFields() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName(null)
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        assertNotEquals(friend1, friend2);
    }

    @Test
    @DisplayName("Should handle equals with self")
    void shouldHandleEqualsWithSelf() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        assertEquals(friend, friend);
    }

    @Test
    @DisplayName("Should handle equals with null")
    void shouldHandleEqualsWithNull() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        assertNotEquals(friend, null);
    }

    @Test
    @DisplayName("Should handle equals with different class")
    void shouldHandleEqualsWithDifferentClass() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        String notAFriend = "Not a Friend object";

        assertNotEquals(friend, notAFriend);
    }

    @Test
    @DisplayName("Should handle hashCode consistency")
    void shouldHandleHashCodeConsistency() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        int hashCode1 = friend.hashCode();
        int hashCode2 = friend.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Should handle canEqual method")
    void shouldHandleCanEqual() {
        Friend friend1 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        Friend friend2 = Friend.builder()
                .id("friend-1")
                .userId("user-1")
                .displayName("John Doe")
                .build();

        assertEquals(friend1, friend2);
        assertTrue(friend1.equals(friend2) && friend2.equals(friend1));
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        Friend friend = Friend.builder()
                .id("friend-1")
                .displayName("John Doe")
                .build();

        String toString = friend.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("John Doe"));
    }
}
