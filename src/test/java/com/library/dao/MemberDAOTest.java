package com.library.dao;

import com.library.model.Member;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberDAOTest {

    private static MemberDAO memberDAO;

    @BeforeAll
    static void setUp() {
        memberDAO = new MemberDAO();
    }

    @Test
    @DisplayName("Should find existing member by ID with calculated active loan and fine stats")
    void shouldFindMemberById() {
        Member member = memberDAO.findById(1);

        assertThat(member).isNotNull();
        assertThat(member.getMembershipNumber()).isEqualTo("LIB-2024-001");
        assertThat(member.getFirstName()).isEqualTo("John");
        assertThat(member.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(member.getActiveLoansCount()).isGreaterThanOrEqualTo(0);
        assertThat(member.getUnpaidFinesTotal()).isNotNull();
    }

    @Test
    @DisplayName("Should find member by membership code")
    void shouldFindByMembershipNumber() {
        Member member = memberDAO.findByMembershipNumber("LIB-2024-001");

        assertThat(member).isNotNull();
        assertThat(member.getId()).isEqualTo(1L);
        assertThat(member.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should return null for non-existent membership number")
    void shouldReturnNullForMissingMember() {
        assertThat(memberDAO.findByMembershipNumber("LIB-NON-EXISTENT")).isNull();
    }

    @Test
    @DisplayName("Should retrieve paginated list and count of members")
    void shouldRetrievePaginatedMembers() {
        long total = memberDAO.count(null, null);
        assertThat(total).isGreaterThanOrEqualTo(3);

        List<Member> list = memberDAO.findAll(null, null, 0, 5);
        assertThat(list).isNotEmpty();
        assertThat(list.size()).isLessThanOrEqualTo(5);
    }
}
